package com.ruleweave.engine

import com.ruleweave.engine.model.EvaluationResult
import com.ruleweave.engine.model.RuleTrace
import java.time.Clock
import java.time.Instant
import java.util.UUID

class RuleEvaluatorImpl<C>(
    private val compiledRules: List<CompiledRule>,
    private val conditionEvaluator: ConditionEvaluator<C>,
    private val actionBuilder: ActionBuilder<C>,
    private val clock: Clock = Clock.systemUTC()
) : RuleEvaluator<C> {

    override fun evaluate(entityId: UUID, context: C): EvaluationResult {
        val startNanos = System.nanoTime()

        val traceResults = compiledRules
            .filter { it.rule.isActive }
            .map { compiled ->
                val evalResult = conditionEvaluator.evaluateWithTrace(compiled.conditions, context)
                Pair(compiled, evalResult)
            }

        val matchedRules = traceResults.filter { (_, evalResult) -> evalResult.matched }

        val actions = matchedRules.map { (compiled, _) -> actionBuilder.build(compiled.rule, context) }

        val traces = traceResults.map { (compiled, evalResult) ->
            RuleTrace(
                ruleName = compiled.rule.name,
                matched = evalResult.matched,
                conditionTraces = evalResult.traces
            )
        }

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        return EvaluationResult(
            entityId = entityId,
            evaluatedRules = compiledRules.count { it.rule.isActive },
            matchedRules = matchedRules.size,
            actions = actions,
            traces = traces,
            evaluationTimeMs = elapsedMs,
            timestamp = Instant.now(clock)
        )
    }
}

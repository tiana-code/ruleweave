package com.ruleweave.engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ruleweave.engine.model.Condition
import com.ruleweave.engine.model.EvaluationResult
import com.ruleweave.engine.model.Rule
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.util.UUID

private val logger = KotlinLogging.logger {}

class RuleEvaluatorImpl<C>(
    rules: List<Rule>,
    fieldResolver: FieldResolver<C>,
    objectMapper: ObjectMapper,
    clock: Clock = Clock.systemUTC()
) : RuleEvaluator<C> {

    private data class CompiledRule(
        val rule: Rule,
        val conditions: List<Condition>
    )

    private val conditionEvaluator = ConditionEvaluator(fieldResolver)
    private val templateRenderer = TemplateRenderer(fieldResolver)
    private val actionBuilder = ActionBuilder(templateRenderer, clock)

    private val compiledRules: List<CompiledRule> = rules.mapNotNull { rule ->
        try {
            val conditions: List<Condition> = objectMapper.readValue(rule.conditionsJson)
            CompiledRule(rule, conditions)
        } catch (e: Exception) {
            logger.error(e) { "Failed to compile rule '${rule.name}': invalid conditionsJson" }
            null
        }
    }

    override fun evaluate(entityId: UUID, context: C): EvaluationResult {
        val startTime = System.currentTimeMillis()

        val matchedRules = compiledRules.filter { compiled ->
            compiled.rule.isActive && conditionEvaluator.evaluate(compiled.conditions, context)
        }

        val actions = matchedRules.map { compiled -> actionBuilder.build(compiled.rule, context) }

        return EvaluationResult(
            entityId = entityId,
            evaluatedRules = compiledRules.count { it.rule.isActive },
            matchedRules = matchedRules.size,
            actions = actions,
            evaluationTimeMs = System.currentTimeMillis() - startTime
        )
    }
}

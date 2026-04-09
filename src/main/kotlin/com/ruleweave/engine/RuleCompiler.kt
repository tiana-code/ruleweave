package com.ruleweave.engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ruleweave.engine.model.Condition
import com.ruleweave.engine.model.Rule

data class CompiledRule(
    val rule: Rule,
    val conditions: List<Condition>
)

data class RuleCompilationResult(
    val compiled: List<CompiledRule>,
    val errors: List<RuleCompilationError>
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

data class RuleCompilationError(
    val ruleName: String,
    val message: String,
    val cause: Exception? = null
)

class RuleCompiler(
    private val objectMapper: ObjectMapper
) {
    fun compile(rules: List<Rule>): RuleCompilationResult {
        val compiled = mutableListOf<CompiledRule>()
        val errors = mutableListOf<RuleCompilationError>()

        for (rule in rules) {
            try {
                val conditions: List<Condition> = objectMapper.readValue(rule.conditionsJson)
                compiled.add(CompiledRule(rule, conditions))
            } catch (e: Exception) {
                errors.add(
                    RuleCompilationError(
                        ruleName = rule.name,
                        message = "Invalid conditionsJson: ${e.message}",
                        cause = e
                    )
                )
            }
        }

        return RuleCompilationResult(compiled, errors)
    }
}

package com.ruleweave.engine.model

data class ConditionTrace(
    val field: String,
    val operator: Operator,
    val expectedValue: Any,
    val actualValue: Any?,
    val matched: Boolean
)

data class RuleTrace(
    val ruleName: String,
    val matched: Boolean,
    val conditionTraces: List<ConditionTrace>
)

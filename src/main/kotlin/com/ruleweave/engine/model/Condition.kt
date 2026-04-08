package com.ruleweave.engine.model

data class Condition(
    val field: String,
    val operator: Operator,
    val value: Any,
    val logicalOperator: LogicalOperator = LogicalOperator.AND
)

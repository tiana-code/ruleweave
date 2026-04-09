package com.ruleweave.engine.model

import java.time.Instant
import java.util.UUID

data class EvaluationResult(
    val entityId: UUID,
    val evaluatedRules: Int,
    val matchedRules: Int,
    val actions: List<RuleActionResult>,
    val traces: List<RuleTrace> = emptyList(),
    val evaluationTimeMs: Long,
    val timestamp: Instant
)

data class RuleActionResult(
    val ruleId: UUID,
    val ruleName: String,
    val priority: Priority,
    val title: String,
    val description: String,
    val recommendedAction: String,
    val confidenceLevel: Double,
    val timeToActionHours: Double?,
    val actionDeadline: Instant?
)

package com.ruleweave.alert.model

import java.time.Instant
import java.util.UUID

data class AlertRule(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val tagCode: String,
    val conditionType: AlertConditionType,
    val conditionThreshold: Double,
    val conditionThresholdHigh: Double? = null,
    val severity: Severity = Severity.WARNING,
    val enabled: Boolean = true,
    val cooldownSeconds: Int = 300,
    val createdAt: Instant = Instant.now()
)

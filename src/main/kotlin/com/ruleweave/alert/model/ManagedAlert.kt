package com.ruleweave.alert.model

import java.time.Instant
import java.util.UUID

data class ManagedAlert(
    val id: UUID = UUID.randomUUID(),
    val ruleId: UUID,
    val entityId: UUID,
    val severity: Severity,
    val tagCode: String,
    val triggerValue: Double,
    val thresholdValue: Double,
    val escalationLevel: Int = 0,
    val slaBreached: Boolean = false,
    val slaDeadline: Instant? = null,
    val assignedTo: String? = null,
    val assignedAt: Instant? = null,
    val startTime: Instant = Instant.now(),
    val acknowledgedAt: Instant? = null
)

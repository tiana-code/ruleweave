package com.ruleweave.alert.model

import java.time.Instant
import java.util.UUID

data class EscalationPolicy(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val severity: Severity,
    val escalationDelayMinutes: Int,
    val escalationTarget: String,
    val maxEscalationLevel: Int = 3,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)

package com.ruleweave.engine.model

import java.time.Instant
import java.util.UUID

data class Rule(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val conditionsJson: String,
    /** JSON-encoded action definitions. Reserved for future use — not yet consumed by the evaluator. */
    val actionsJson: String,
    val priority: Int = 100,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

package com.ruleweave.engine

import com.ruleweave.engine.model.Priority
import com.ruleweave.engine.model.Rule
import com.ruleweave.engine.model.RuleActionResult
import java.time.Clock
import java.time.Instant

class ActionBuilder<C>(
    private val templateRenderer: TemplateRenderer<C>,
    private val clock: Clock = Clock.systemUTC()
) {

    fun build(rule: Rule, context: C): RuleActionResult {
        val priority = inferPriority(rule)
        return RuleActionResult(
            ruleId = rule.id,
            ruleName = rule.name,
            priority = priority,
            title = templateRenderer.render(rule.name, context),
            description = templateRenderer.render(rule.description ?: "", context),
            recommendedAction = "Review rule '${rule.name}' and take appropriate action",
            confidenceLevel = 1.0,
            timeToActionHours = timeToActionFromPriority(priority),
            actionDeadline = deadlineFromPriority(priority)
        )
    }

    private fun inferPriority(rule: Rule): Priority = when {
        rule.priority <= 10 -> Priority.CRITICAL
        rule.priority <= 30 -> Priority.HIGH
        rule.priority <= 60 -> Priority.MEDIUM
        rule.priority <= 80 -> Priority.LOW
        else -> Priority.INFO
    }

    private fun timeToActionFromPriority(priority: Priority): Double? = when (priority) {
        Priority.CRITICAL -> 12.0
        Priority.HIGH -> 24.0
        Priority.MEDIUM -> 72.0
        Priority.LOW -> null
        Priority.INFO -> null
    }

    private fun deadlineFromPriority(priority: Priority): Instant? {
        val hours = timeToActionFromPriority(priority) ?: return null
        return Instant.now(clock).plusSeconds((hours * 3600).toLong())
    }
}

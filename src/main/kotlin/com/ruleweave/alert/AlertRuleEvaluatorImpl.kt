package com.ruleweave.alert

import com.ruleweave.alert.model.AlertConditionType
import com.ruleweave.alert.model.AlertRule
import com.ruleweave.alert.model.ManagedAlert
import java.util.UUID

class AlertRuleEvaluatorImpl(
    private val rules: List<AlertRule>
) : AlertRuleEvaluator {

    override fun evaluate(entityId: UUID, tagCode: String, value: Double): List<ManagedAlert> {
        return rules
            .filter { it.enabled && it.tagCode == tagCode }
            .filter { shouldTrigger(it, value) }
            .map { rule ->
                ManagedAlert(
                    ruleId = rule.id,
                    entityId = entityId,
                    severity = rule.severity,
                    tagCode = tagCode,
                    triggerValue = value,
                    thresholdValue = rule.conditionThreshold
                )
            }
    }

    override fun shouldTrigger(rule: AlertRule, value: Double): Boolean {
        if (!rule.enabled) return false
        return when (rule.conditionType) {
            AlertConditionType.GREATER_THAN -> value > rule.conditionThreshold
            AlertConditionType.LESS_THAN -> value < rule.conditionThreshold
            AlertConditionType.EQUALS -> value.compareTo(rule.conditionThreshold) == 0
            AlertConditionType.NOT_EQUALS -> value.compareTo(rule.conditionThreshold) != 0
            AlertConditionType.OUT_OF_RANGE -> {
                val high = rule.conditionThresholdHigh ?: return false
                value < rule.conditionThreshold || value > high
            }

            AlertConditionType.STALE -> value > rule.conditionThreshold
            AlertConditionType.DELTA -> value > rule.conditionThreshold
            AlertConditionType.RATE_OF_CHANGE -> value > rule.conditionThreshold
        }
    }
}

package com.ruleweave.alert

import com.ruleweave.alert.model.AlertRule
import com.ruleweave.alert.model.ManagedAlert
import java.util.UUID

interface AlertRuleEvaluator {

    fun evaluate(entityId: UUID, tagCode: String, value: Double): List<ManagedAlert>

    fun shouldTrigger(rule: AlertRule, value: Double): Boolean
}

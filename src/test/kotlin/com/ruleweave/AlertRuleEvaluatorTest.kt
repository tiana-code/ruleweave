package com.ruleweave

import com.ruleweave.alert.AlertRuleEvaluatorImpl
import com.ruleweave.alert.model.AlertConditionType
import com.ruleweave.alert.model.AlertRule
import com.ruleweave.alert.model.Severity
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlertRuleEvaluatorTest {

    private fun rule(
        conditionType: AlertConditionType,
        threshold: Double,
        thresholdHigh: Double? = null,
        tagCode: String = "engine.rpm",
        severity: Severity = Severity.WARNING,
        enabled: Boolean = true
    ) = AlertRule(
        name = "test-alert",
        tagCode = tagCode,
        conditionType = conditionType,
        conditionThreshold = threshold,
        conditionThresholdHigh = thresholdHigh,
        severity = severity,
        enabled = enabled
    )

    private fun evaluator(vararg rules: AlertRule) = AlertRuleEvaluatorImpl(rules.toList())

    @Test
    fun `GREATER_THAN triggers when value exceeds threshold`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 1000.0)
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 1200.0))
    }

    @Test
    fun `GREATER_THAN does not trigger when value equals threshold`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 1000.0)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 1000.0))
    }

    @Test
    fun `GREATER_THAN does not trigger when value is below threshold`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 1000.0)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 800.0))
    }

    @Test
    fun `LESS_THAN triggers when value is below threshold`() {
        val ruleVar = rule(AlertConditionType.LESS_THAN, threshold = 10.0, tagCode = "fuel.level")
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 5.0))
    }

    @Test
    fun `LESS_THAN does not trigger when value equals threshold`() {
        val ruleVar = rule(AlertConditionType.LESS_THAN, threshold = 10.0, tagCode = "fuel.level")
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 10.0))
    }

    @Test
    fun `LESS_THAN does not trigger when value is above threshold`() {
        val ruleVar = rule(AlertConditionType.LESS_THAN, threshold = 10.0, tagCode = "fuel.level")
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 50.0))
    }

    @Test
    fun `EQUALS triggers when value matches threshold exactly`() {
        val ruleVar = rule(AlertConditionType.EQUALS, threshold = 0.0, tagCode = "engine.state")
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 0.0))
    }

    @Test
    fun `EQUALS does not trigger when value differs from threshold`() {
        val ruleVar = rule(AlertConditionType.EQUALS, threshold = 0.0, tagCode = "engine.state")
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 1.0))
    }

    @Test
    fun `NOT_EQUALS triggers when value differs from threshold`() {
        val ruleVar = rule(AlertConditionType.NOT_EQUALS, threshold = 1.0, tagCode = "override.flag")
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 0.0))
    }

    @Test
    fun `NOT_EQUALS does not trigger when value matches threshold`() {
        val ruleVar = rule(AlertConditionType.NOT_EQUALS, threshold = 1.0, tagCode = "override.flag")
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 1.0))
    }

    @Test
    fun `OUT_OF_RANGE triggers when value is below lower bound`() {
        val ruleVar = rule(AlertConditionType.OUT_OF_RANGE, threshold = 10.0, thresholdHigh = 90.0)
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 5.0))
    }

    @Test
    fun `OUT_OF_RANGE triggers when value is above upper bound`() {
        val ruleVar = rule(AlertConditionType.OUT_OF_RANGE, threshold = 10.0, thresholdHigh = 90.0)
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 95.0))
    }

    @Test
    fun `OUT_OF_RANGE does not trigger for value within bounds`() {
        val ruleVar = rule(AlertConditionType.OUT_OF_RANGE, threshold = 10.0, thresholdHigh = 90.0)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 50.0))
    }

    @Test
    fun `OUT_OF_RANGE does not trigger on lower boundary value`() {
        val ruleVar = rule(AlertConditionType.OUT_OF_RANGE, threshold = 10.0, thresholdHigh = 90.0)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 10.0))
    }

    @Test
    fun `OUT_OF_RANGE does not trigger on upper boundary value`() {
        val ruleVar = rule(AlertConditionType.OUT_OF_RANGE, threshold = 10.0, thresholdHigh = 90.0)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 90.0))
    }

    @Test
    fun `OUT_OF_RANGE with missing thresholdHigh never triggers`() {
        val ruleVar = rule(AlertConditionType.OUT_OF_RANGE, threshold = 10.0, thresholdHigh = null)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 5.0))
    }

    @Test
    fun `STALE triggers when age value exceeds threshold seconds`() {
        val ruleVar = rule(AlertConditionType.STALE, threshold = 300.0, tagCode = "gps.fix.age")
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 600.0))
    }

    @Test
    fun `STALE does not trigger when age value is within threshold`() {
        val ruleVar = rule(AlertConditionType.STALE, threshold = 300.0, tagCode = "gps.fix.age")
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 100.0))
    }

    @Test
    fun `STALE type falls back to threshold comparison`() {
        val ruleVar = rule(AlertConditionType.STALE, threshold = 60.0, tagCode = "sensor.age")
        assertTrue(evaluator(ruleVar).shouldTrigger(ruleVar, 61.0))
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 60.0))
    }

    @Test
    fun `disabled rule does not trigger regardless of value`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 10.0, enabled = false)
        assertFalse(evaluator(ruleVar).shouldTrigger(ruleVar, 500.0))
    }

    @Test
    fun `evaluate returns empty list when no rules match the tagCode`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 1000.0, tagCode = "engine.rpm")
        val alerts = evaluator(ruleVar).evaluate(UUID.randomUUID(), "fuel.level", 500.0)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `evaluate returns empty list when value does not exceed threshold`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 1000.0, tagCode = "engine.rpm")
        val alerts = evaluator(ruleVar).evaluate(UUID.randomUUID(), "engine.rpm", 500.0)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `evaluate returns ManagedAlert when rule triggers`() {
        val entityId = UUID.randomUUID()
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 1000.0, tagCode = "engine.rpm")
        val alerts = evaluator(ruleVar).evaluate(entityId, "engine.rpm", 1200.0)
        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertEquals(entityId, alert.entityId)
        assertEquals(ruleVar.id, alert.ruleId)
        assertEquals("engine.rpm", alert.tagCode)
        assertEquals(1200.0, alert.triggerValue)
        assertEquals(1000.0, alert.thresholdValue)
        assertEquals(Severity.WARNING, alert.severity)
    }

    @Test
    fun `evaluate returns CRITICAL severity when rule severity is CRITICAL`() {
        val entityId = UUID.randomUUID()
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 100.0, severity = Severity.CRITICAL)
        val alerts = evaluator(ruleVar).evaluate(entityId, "engine.rpm", 120.0)
        assertEquals(1, alerts.size)
        assertEquals(Severity.CRITICAL, alerts.first().severity)
    }

    @Test
    fun `evaluate skips disabled rules even when value exceeds threshold`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 100.0, enabled = false)
        val alerts = evaluator(ruleVar).evaluate(UUID.randomUUID(), "engine.rpm", 500.0)
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `evaluate returns one alert per matching rule`() {
        val entityId = UUID.randomUUID()
        val r1 = rule(
            AlertConditionType.GREATER_THAN,
            threshold = 100.0,
            tagCode = "engine.rpm",
            severity = Severity.WARNING
        )
        val r2 = rule(
            AlertConditionType.GREATER_THAN,
            threshold = 200.0,
            tagCode = "engine.rpm",
            severity = Severity.CRITICAL
        )
        val alerts = evaluator(r1, r2).evaluate(entityId, "engine.rpm", 300.0)
        assertEquals(2, alerts.size)
    }

    @Test
    fun `evaluate only triggers rules whose tagCode matches`() {
        val entityId = UUID.randomUUID()
        val rpmRule = rule(AlertConditionType.GREATER_THAN, threshold = 100.0, tagCode = "engine.rpm")
        val fuelRule = rule(AlertConditionType.LESS_THAN, threshold = 10.0, tagCode = "fuel.level")
        val alerts = evaluator(rpmRule, fuelRule).evaluate(entityId, "engine.rpm", 200.0)
        assertEquals(1, alerts.size)
        assertEquals("engine.rpm", alerts.first().tagCode)
    }

    @Test
    fun `ManagedAlert produced by evaluate has slaBreached false and escalationLevel zero`() {
        val ruleVar = rule(AlertConditionType.GREATER_THAN, threshold = 100.0)
        val alerts = evaluator(ruleVar).evaluate(UUID.randomUUID(), "engine.rpm", 200.0)
        assertEquals(1, alerts.size)
        assertFalse(alerts.first().slaBreached)
        assertEquals(0, alerts.first().escalationLevel)
    }
}

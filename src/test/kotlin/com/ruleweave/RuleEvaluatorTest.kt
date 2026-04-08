package com.ruleweave

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ruleweave.engine.FieldResolver
import com.ruleweave.engine.RuleEvaluatorImpl
import com.ruleweave.engine.model.Priority
import com.ruleweave.engine.model.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleEvaluatorTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    private val mapResolver = FieldResolver<Map<String, Any?>> { field, ctx -> ctx[field] }

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    private fun evaluator(vararg rules: Rule) =
        RuleEvaluatorImpl(rules.toList(), mapResolver, objectMapper, fixedClock)

    private fun rule(
        conditionsJson: String,
        priority: Int = 50,
        isActive: Boolean = true,
        name: String = "test-rule"
    ) = Rule(
        name = name,
        conditionsJson = conditionsJson,
        actionsJson = "[]",
        priority = priority,
        isActive = isActive
    )

    private fun ctx(vararg pairs: Pair<String, Any?>) = mapOf(*pairs)

    @BeforeEach
    fun setUp() {
    }

    @Test
    fun `EQUALS matches identical string values`() {
        val ruleVar = rule("""[{"field":"status","operator":"EQUALS","value":"active"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("status" to "active"))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `EQUALS does not match different values`() {
        val ruleVar = rule("""[{"field":"status","operator":"EQUALS","value":"active"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("status" to "idle"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `NOT_EQUALS matches when values differ`() {
        val ruleVar = rule("""[{"field":"status","operator":"NOT_EQUALS","value":"offline"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("status" to "active"))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `NOT_EQUALS does not match when values are equal`() {
        val ruleVar = rule("""[{"field":"status","operator":"NOT_EQUALS","value":"offline"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("status" to "offline"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `GREATER_THAN matches when field exceeds threshold`() {
        val ruleVar = rule("""[{"field":"speed","operator":"GREATER_THAN","value":20}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 25.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `GREATER_THAN does not match when field equals threshold`() {
        val ruleVar = rule("""[{"field":"speed","operator":"GREATER_THAN","value":20}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 20.0))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `GREATER_THAN does not match when field is below threshold`() {
        val ruleVar = rule("""[{"field":"speed","operator":"GREATER_THAN","value":20}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 15.0))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `non-numeric string in GREATER_THAN returns false`() {
        val ruleVar = rule("""[{"field":"speed","operator":"GREATER_THAN","value":10}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to "abc"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `null field returns false for comparison operators`() {
        val ruleVar = rule("""[{"field":"speed","operator":"GREATER_THAN","value":10}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to null))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `LESS_THAN matches when field is below threshold`() {
        val ruleVar = rule("""[{"field":"fuel","operator":"LESS_THAN","value":15}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("fuel" to 10.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `LESS_THAN does not match when field equals threshold`() {
        val ruleVar = rule("""[{"field":"fuel","operator":"LESS_THAN","value":15}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("fuel" to 15.0))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `GREATER_THAN_OR_EQUALS matches when field equals threshold`() {
        val ruleVar = rule("""[{"field":"load","operator":"GREATER_THAN_OR_EQUALS","value":80}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("load" to 80.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `LESS_THAN_OR_EQUALS matches when field equals threshold`() {
        val ruleVar = rule("""[{"field":"load","operator":"LESS_THAN_OR_EQUALS","value":80}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("load" to 80.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `CONTAINS matches substring`() {
        val ruleVar = rule("""[{"field":"message","operator":"CONTAINS","value":"error"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("message" to "critical error occurred"))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `CONTAINS does not match when substring absent`() {
        val ruleVar = rule("""[{"field":"message","operator":"CONTAINS","value":"error"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("message" to "all systems nominal"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `IN matches when value is in comma-separated list`() {
        val ruleVar = rule("""[{"field":"port","operator":"IN","value":"Hamburg,Rotterdam,Antwerp"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("port" to "Rotterdam"))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `IN does not match when value is absent from list`() {
        val ruleVar = rule("""[{"field":"port","operator":"IN","value":"Hamburg,Rotterdam,Antwerp"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("port" to "Oslo"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `NOT_IN matches when value is absent from list`() {
        val ruleVar = rule("""[{"field":"zone","operator":"NOT_IN","value":"restricted,danger"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("zone" to "open-sea"))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `NOT_IN does not match when value is in list`() {
        val ruleVar = rule("""[{"field":"zone","operator":"NOT_IN","value":"restricted,danger"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("zone" to "danger"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `BETWEEN matches when value is within inclusive range`() {
        val ruleVar = rule("""[{"field":"temp","operator":"BETWEEN","value":[10,30]}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("temp" to 20.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `BETWEEN matches on lower bound`() {
        val ruleVar = rule("""[{"field":"temp","operator":"BETWEEN","value":[10,30]}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("temp" to 10.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `BETWEEN matches on upper bound`() {
        val ruleVar = rule("""[{"field":"temp","operator":"BETWEEN","value":[10,30]}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("temp" to 30.0))
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `BETWEEN does not match outside range`() {
        val ruleVar = rule("""[{"field":"temp","operator":"BETWEEN","value":[10,30]}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("temp" to 35.0))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `AND requires both conditions to match`() {
        val ruleVar = rule(
            """[
                {"field":"speed","operator":"GREATER_THAN","value":10},
                {"field":"fuel","operator":"LESS_THAN","value":20,"logicalOperator":"AND"}
            ]"""
        )
        val bothMatch = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 15.0, "fuel" to 10.0))
        assertEquals(1, bothMatch.matchedRules)

        val onlyFirst = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 15.0, "fuel" to 50.0))
        assertEquals(0, onlyFirst.matchedRules)
    }

    @Test
    fun `OR matches when at least one condition is true`() {
        val ruleVar = rule(
            """[
                {"field":"speed","operator":"GREATER_THAN","value":30},
                {"field":"fuel","operator":"LESS_THAN","value":5,"logicalOperator":"OR"}
            ]"""
        )
        val firstOnly = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 35.0, "fuel" to 50.0))
        assertEquals(1, firstOnly.matchedRules)

        val secondOnly = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 5.0, "fuel" to 3.0))
        assertEquals(1, secondOnly.matchedRules)

        val neither = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 5.0, "fuel" to 50.0))
        assertEquals(0, neither.matchedRules)
    }

    @Test
    fun `three-condition AND chain fails when middle condition is false`() {
        val ruleVar = rule(
            """[
                {"field":"a","operator":"EQUALS","value":"x"},
                {"field":"b","operator":"EQUALS","value":"y","logicalOperator":"AND"},
                {"field":"c","operator":"EQUALS","value":"z","logicalOperator":"AND"}
            ]"""
        )
        val allMatch = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("a" to "x", "b" to "y", "c" to "z"))
        assertEquals(1, allMatch.matchedRules)

        val middleFails = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("a" to "x", "b" to "WRONG", "c" to "z"))
        assertEquals(0, middleFails.matchedRules)
    }

    @Test
    fun `short-circuit AND skips right side when left is false`() {
        var rightSideAccessed = false
        val throwingResolver = FieldResolver<Map<String, Any?>> { field, ctx ->
            if (field == "risky") {
                rightSideAccessed = true
                throw IllegalStateException("should not be accessed")
            }
            ctx[field]
        }
        val ruleVar = Rule(
            name = "short-circuit-test",
            conditionsJson = """[
                {"field":"flag","operator":"EQUALS","value":"expected"},
                {"field":"risky","operator":"EQUALS","value":"x","logicalOperator":"AND"}
            ]""",
            actionsJson = "[]",
            priority = 50
        )
        val eval = RuleEvaluatorImpl(listOf(ruleVar), throwingResolver, objectMapper, fixedClock)
        val result = eval.evaluate(UUID.randomUUID(), mapOf("flag" to "not-expected"))
        assertEquals(0, result.matchedRules)
        assertFalse(rightSideAccessed)
    }

    @Test
    fun `missing field resolves to null and condition evaluates to false`() {
        val ruleVar = rule("""[{"field":"nonexistent","operator":"EQUALS","value":"anything"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("other" to "value"))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `null field value resolves to false`() {
        val ruleVar = rule("""[{"field":"field","operator":"EQUALS","value":"value"}]""")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("field" to null))
        assertEquals(0, result.matchedRules)
    }

    @Test
    fun `inactive rule is not evaluated`() {
        val active = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", isActive = true)
        val inactive = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", isActive = false)
        val result = evaluator(active, inactive).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(1, result.evaluatedRules)
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `empty condition list matches unconditionally`() {
        val ruleVar = rule("[]")
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx())
        assertEquals(1, result.matchedRules)
    }

    @Test
    fun `priority 5 maps to CRITICAL`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 5)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(Priority.CRITICAL, result.actions.first().priority)
    }

    @Test
    fun `priority 20 maps to HIGH`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 20)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(Priority.HIGH, result.actions.first().priority)
    }

    @Test
    fun `priority 50 maps to MEDIUM`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 50)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(Priority.MEDIUM, result.actions.first().priority)
    }

    @Test
    fun `priority 70 maps to LOW`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 70)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(Priority.LOW, result.actions.first().priority)
    }

    @Test
    fun `priority 100 maps to INFO`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 100)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(Priority.INFO, result.actions.first().priority)
    }

    @Test
    fun `template variables in rule name are interpolated from context`() {
        val ruleVar = Rule(
            name = "Speed alert for vessel {{vesselId}}",
            conditionsJson = """[{"field":"speed","operator":"GREATER_THAN","value":10}]""",
            actionsJson = "[]",
            priority = 50
        )
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("speed" to 15.0, "vesselId" to "V-42"))
        assertEquals("Speed alert for vessel V-42", result.actions.first().title)
    }

    @Test
    fun `missing template variable is replaced with empty string`() {
        val ruleVar = Rule(
            name = "Alert {{missing}}",
            conditionsJson = """[{"field":"x","operator":"EQUALS","value":"1"}]""",
            actionsJson = "[]",
            priority = 50
        )
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals("Alert ", result.actions.first().title)
    }

    @Test
    fun `CRITICAL priority produces SLA deadline 12 hours from fixed clock`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 5)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))

        val deadline = result.actions.first().actionDeadline!!
        val expectedDeadline = Instant.parse("2026-01-01T12:00:00Z")
        assertEquals(expectedDeadline, deadline)
    }

    @Test
    fun `INFO priority produces no SLA deadline`() {
        val ruleVar = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""", priority = 100)
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertFalse(result.actions.any { it.actionDeadline != null })
    }

    @Test
    fun `evaluation result tracks correct counts`() {
        val match = rule("""[{"field":"x","operator":"EQUALS","value":"1"}]""")
        val noMatch = rule("""[{"field":"x","operator":"EQUALS","value":"2"}]""")
        val result = evaluator(match, noMatch).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(2, result.evaluatedRules)
        assertEquals(1, result.matchedRules)
        assertTrue(result.evaluationTimeMs >= 0)
    }

    @Test
    fun `invalid conditionsJson does not throw but skips rule`() {
        val ruleVar = Rule(
            name = "bad-json",
            conditionsJson = "not valid json",
            actionsJson = "[]",
            priority = 50
        )
        val result = evaluator(ruleVar).evaluate(UUID.randomUUID(), ctx("x" to "1"))
        assertEquals(0, result.matchedRules)
    }
}

package com.ruleweave.engine

import com.ruleweave.engine.model.Condition
import com.ruleweave.engine.model.LogicalOperator
import com.ruleweave.engine.model.Operator
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class ConditionEvaluator<C>(
    private val fieldResolver: FieldResolver<C>
) {

    fun evaluate(conditions: List<Condition>, context: C): Boolean {
        if (conditions.isEmpty()) return true

        var result = evaluateSingle(conditions.first(), context)

        for (i in 1 until conditions.size) {
            val condition = conditions[i]
            result = when (condition.logicalOperator) {
                LogicalOperator.AND -> if (!result) false else evaluateSingle(condition, context)
                LogicalOperator.OR -> if (result) true else evaluateSingle(condition, context)
            }
        }

        return result
    }

    private fun evaluateSingle(condition: Condition, context: C): Boolean {
        val fieldValue = fieldResolver.resolve(condition.field, context) ?: return false
        val conditionValue = condition.value

        return try {
            when (condition.operator) {
                Operator.EQUALS -> {
                    val actualNum = toDoubleOrNull(fieldValue)
                    val expectedNum = toDoubleOrNull(conditionValue)
                    if (actualNum != null && expectedNum != null) actualNum == expectedNum
                    else fieldValue.toString() == conditionValue.toString()
                }

                Operator.NOT_EQUALS -> {
                    val actualNum = toDoubleOrNull(fieldValue)
                    val expectedNum = toDoubleOrNull(conditionValue)
                    if (actualNum != null && expectedNum != null) actualNum != expectedNum
                    else fieldValue.toString() != conditionValue.toString()
                }

                Operator.GREATER_THAN -> compareNumeric(fieldValue, conditionValue)?.let { it > 0 } ?: false
                Operator.LESS_THAN -> compareNumeric(fieldValue, conditionValue)?.let { it < 0 } ?: false
                Operator.GREATER_THAN_OR_EQUALS -> compareNumeric(fieldValue, conditionValue)?.let { it >= 0 } ?: false
                Operator.LESS_THAN_OR_EQUALS -> compareNumeric(fieldValue, conditionValue)?.let { it <= 0 } ?: false
                Operator.CONTAINS -> fieldValue.toString().contains(conditionValue.toString())
                Operator.IN -> {
                    val values = toStringList(conditionValue)
                    fieldValue.toString() in values
                }

                Operator.NOT_IN -> {
                    val values = toStringList(conditionValue)
                    fieldValue.toString() !in values
                }

                Operator.BETWEEN -> {
                    val range = when (conditionValue) {
                        is List<*> -> conditionValue.mapNotNull { toDoubleOrNull(it) }
                        is String -> conditionValue.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                        else -> emptyList()
                    }
                    val numValue = toDoubleOrNull(fieldValue)
                    if (range.size >= 2 && numValue != null) {
                        numValue >= range[0] && numValue <= range[1]
                    } else false
                }
            }
        } catch (e: Exception) {
            logger.warn { "Failed to evaluate condition: ${condition.field} ${condition.operator} ${condition.value}" }
            false
        }
    }

    private fun toStringList(value: Any): List<String> = when (value) {
        is List<*> -> value.map { it.toString() }
        is String -> value.split(",").map { it.trim() }
        else -> listOf(value.toString())
    }

    private fun compareNumeric(actual: Any, expected: Any): Int? {
        val actualNumber = toDoubleOrNull(actual) ?: return null
        val expectedNumber = toDoubleOrNull(expected) ?: return null
        return actualNumber.compareTo(expectedNumber)
    }

    private fun toDoubleOrNull(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

package com.ruleweave.alert.model

enum class AlertConditionType {
    GREATER_THAN,
    LESS_THAN,
    EQUALS,
    NOT_EQUALS,
    DELTA,
    RATE_OF_CHANGE,
    OUT_OF_RANGE,
    STALE
}

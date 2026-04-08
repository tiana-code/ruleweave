package com.ruleweave.alert.model

enum class Severity {
    INFO,
    WARNING,
    CRITICAL;

    companion object {
        fun fromString(value: String?): Severity = when (value?.lowercase()) {
            "critical" -> CRITICAL
            "warning" -> WARNING
            "info" -> INFO
            else -> WARNING
        }
    }
}

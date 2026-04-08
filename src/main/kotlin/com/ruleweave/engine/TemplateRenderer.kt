package com.ruleweave.engine

class TemplateRenderer<C>(
    private val fieldResolver: FieldResolver<C>
) {

    private val pattern = Regex("\\{\\{(\\w+)}}")

    fun render(template: String, context: C): String {
        var result = template
        pattern.findAll(template).forEach { match ->
            val field = match.groupValues[1]
            val value = fieldResolver.resolve(field, context)?.toString() ?: ""
            result = result.replace(match.value, value)
        }
        return result
    }
}

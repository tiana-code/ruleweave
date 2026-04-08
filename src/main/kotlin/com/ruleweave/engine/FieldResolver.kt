package com.ruleweave.engine

fun interface FieldResolver<C> {

    fun resolve(field: String, context: C): Any?
}

package com.ruleweave.engine

import com.ruleweave.engine.model.EvaluationResult
import java.util.UUID

interface RuleEvaluator<C> {

    fun evaluate(entityId: UUID, context: C): EvaluationResult
}

package org.partiql.lang.eval.physical.window

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.operators.ValueExpression
import org.partiql.lang.eval.physical.operators.transferState
import org.partiql.lang.eval.physical.toSetVariableFunc

// Type Influencer for window function is not implemented yet.
// internal fun createBuiltInWindowFunctionSignatures(): Map<String, WindowFunctionSignature> =
//    createBuiltinWindowFunctions(ExprValueFactory.standard(IonSystemBuilder.standard().build()))
//        .map { it.signature }
//        .associateBy{ it.name }

internal fun createBuiltinWindowFunctions() =
    listOf(
        Lag(),
        Lead()
    )
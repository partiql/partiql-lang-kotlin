package org.partiql.lang.eval.physical.window

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

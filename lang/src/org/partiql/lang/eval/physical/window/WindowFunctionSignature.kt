package org.partiql.lang.eval.physical.window

/**
 * Window function signature
 *
 * For now just holding function name
 *
 * In the future we can add additional parameter such as frame information here.
 */
class WindowFunctionSignature(
    val name: String
)

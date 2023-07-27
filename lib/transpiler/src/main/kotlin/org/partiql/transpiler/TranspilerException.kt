package org.partiql.transpiler

/**
 * Top-level wrapper of any fatal problem.
 */
class TranspilerException(
    override val message: String?,
    override val cause: Throwable?,
) : Exception()

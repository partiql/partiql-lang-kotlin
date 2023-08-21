package org.partiql.transpiler

/**
 * Top-level wrapper of any fatal problem.
 */
class TranspilerException(
    override val message: String?,
    override val cause: Throwable?,
) : Exception()

/**
 * Simple handler
 */
typealias ProblemCallback = (TranspilerProblem) -> Unit

/**
 * A place to record transpilation oddities
 *
 * @property level
 * @property message
 */
class TranspilerProblem(val level: Level, val message: String) {

    enum class Level {
        INFO,
        WARNING,
        ERROR,
    }

    override fun toString() = "$level: $message"
}

package org.partiql.transpiler

/**
 * Top-level wrapper of any fatal problem.
 */
class TranspilerException(
    override val message: String?,
    override val cause: Throwable?,
) : Exception()

/**
 * Simple handler. The extension methods make me think I've recreated a class..
 */
typealias ProblemCallback = (TranspilerProblem) -> Unit

public fun ProblemCallback.info(message: String) = this(
    TranspilerProblem(
        level = TranspilerProblem.Level.INFO,
        message = message
    )
)

public fun ProblemCallback.warn(message: String) = this(
    TranspilerProblem(
        level = TranspilerProblem.Level.WARNING,
        message = message
    )
)

public fun ProblemCallback.error(message: String) = this(
    TranspilerProblem(
        level = TranspilerProblem.Level.ERROR,
        message = message
    )
)

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

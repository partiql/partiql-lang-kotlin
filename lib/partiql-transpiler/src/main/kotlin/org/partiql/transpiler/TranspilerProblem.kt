package org.partiql.transpiler

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
}

package org.partiql.errors

/**
 * Typeof `ProblemHandler.handleProblem`
 */
public typealias ProblemCallback = (Problem) -> Unit

/**
 * In general, a [Problem] is a semantic error or warning encountered during compilation of a query.
 *
 * @param sourceLocation stores the location (line and column) in the query the problem occurred
 * @param details details related to the problem's severity and a human-readable message
 */
public data class Problem(val sourceLocation: ProblemLocation, val details: ProblemDetails) {
    override fun toString(): String = "$sourceLocation: ${details.message}"
}

/**
 * Represents a specific location within a statement's source text.
 */
public data class ProblemLocation(
    public val lineNum: Long,
    public val charOffset: Long,
    public val length: Long = -1,
) {

    override fun toString(): String = "$lineNum:$charOffset:${if (length > 0) length.toString() else "<unknown>"}"
}

/**
 * Analogous to the UNKNOWN_SOURCE_LOCATION
 */
public val UNKNOWN_PROBLEM_LOCATION: ProblemLocation = ProblemLocation(-1, -1, -1)

/**
 * Property of [ProblemDetails] that represents the severity level.
 *
 * As a general guideline (from least to most severe),
 * - [WARNING] - query can run but may have some unintentional behavior (e.g. operation in a query
 * always returns a constant).
 */
public enum class ProblemSeverity {
    WARNING,
    ERROR
}

/**
 * Info related to the problem's severity and a human-readable message.
 */
public interface ProblemDetails {
    public val severity: ProblemSeverity
    public val message: String
}

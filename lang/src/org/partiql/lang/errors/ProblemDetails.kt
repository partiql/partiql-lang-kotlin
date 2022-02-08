package org.partiql.lang.errors

/**
 * Property of [ProblemDetails] that represents the severity level.
 *
 * As a general guideline (from least to most severe),
 * - [ProblemSeverity.WARNING] - query can run but may have some unintentional behavior (e.g. operation in a query
 * always returns a constant).
 * - [ProblemSeverity.ERROR] - query cannot run due to the encountered problem.
 */
enum class ProblemSeverity {
    WARNING,
    ERROR;
}

/**
 * Info related to the problem's severity and a human-readable message.
 */
interface ProblemDetails {
    val severity: ProblemSeverity
    val message: String
}

package org.partiql.lang.errors

import org.partiql.lang.ast.SourceLocationMeta

/**
 * In general, a [Problem] is a semantic error or warning encountered during compilation of a query.
 *
 * @param sourceLocation stores the location (line and column) in the query the problem occurred
 * @param details details related to the problem's severity and a human-readable message
 */
data class Problem(val sourceLocation: SourceLocationMeta, val details: ProblemDetails)

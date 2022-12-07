package org.partiql.lang.syntax

/**
 * Defines the location of a token and its length in bytes.
 */
@Deprecated(
    message = "This class is subject to removal.",
    level = DeprecationLevel.WARNING
) // To be removed before 1.0
data class SourceSpan(val line: Long, val column: Long, val length: Long)

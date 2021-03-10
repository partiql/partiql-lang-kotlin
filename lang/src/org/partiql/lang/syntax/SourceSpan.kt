package org.partiql.lang.syntax

/**
 * Defines the location of a token and its length in bytes.
 */
data class SourceSpan(val line: Long, val column: Long, val length: Long)

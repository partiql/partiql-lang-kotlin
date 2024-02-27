package org.partiql.errors

/**
 * A [TypeCheckException] represents an invalid operation due to argument types.
 */
public class TypeCheckException(message: String? = null) : RuntimeException(message)

/**
 * A [DataException] represents an unrecoverable query runtime exception.
 */
public class DataException(public override val message: String) : RuntimeException()

/**
 * A [CardinalityViolation] represents an invalid operation due to an unexpected cardinality of an argument.
 *
 * From SQL:1999:
 * > If the cardinality of a <row subquery> is greater than 1 (one), then an exception condition is raised: cardinality violation.
 * > If the cardinality of SS [[scalar subquery]] is greater than 1 (one), then an exception condition is raised: cardinality violation.
 */
public class CardinalityViolation : RuntimeException()

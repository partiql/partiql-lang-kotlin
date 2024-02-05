package org.partiql.errors

/**
 * A [TypeCheckException] represents an invalid operation due to argument types.
 */
public class TypeCheckException : RuntimeException()

/**
 * A [DataException] represents an unrecoverable query runtime exception.
 */
public class DataException(public override val message: String) : RuntimeException()

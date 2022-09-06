package org.partiql.lang.errors

/**
 * Base class for PartiQL Exceptions
 */
class PartiQLException(override val message: String) : RuntimeException()

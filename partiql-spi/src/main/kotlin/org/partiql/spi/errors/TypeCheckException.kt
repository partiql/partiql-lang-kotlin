package org.partiql.spi.errors

/**
 * A [TypeCheckException] represents an invalid operation due to argument types.
 */
public class TypeCheckException(message: String? = null) : RuntimeException(message) {

    /**
     * This does not provide the stack trace, as this is very expensive in permissive mode.
     */
    override fun fillInStackTrace(): Throwable = this
}

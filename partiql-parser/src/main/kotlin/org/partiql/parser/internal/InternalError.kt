package org.partiql.parser.internal

import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorCode
import org.partiql.spi.errors.Property

internal class InternalError(
    private val cause: Throwable,
) : Error {

    override fun getCode(): Int = ErrorCode.INTERNAL_ERROR

    override fun getProperty(key: Int): Any? = when (key) {
        Property.CAUSE -> cause.javaClass.simpleName
        else -> null
    }

    override fun getProperties(): MutableCollection<Int> {
        return mutableSetOf(Property.CAUSE)
    }
}

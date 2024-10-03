package org.partiql.parser.internal

import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorCode
import org.partiql.spi.errors.Property

internal class LexerError(
    private val line: Int?,
    private val column: Int?,
    private val length: Int?,
    private val tokenName: String,
    private val tokenContent: String
) : Error {

    override fun getCode(): Int = ErrorCode.UNEXPECTED_TOKEN

    override fun getProperty(key: Int): Any? = when (key) {
        Property.LINE_NO -> line
        Property.COLUMN_NO -> column
        Property.LENGTH -> length
        Property.TOKEN_NAME -> tokenName
        Property.TOKEN_CONTENT -> tokenContent
        else -> null
    }

    override fun getProperties(): MutableCollection<Int> {
        return setOfNotNull(
            Property.LINE_NO,
            Property.COLUMN_NO,
            Property.LENGTH,
            Property.TOKEN_NAME,
            Property.TOKEN_CONTENT
        ).toMutableSet()
    }
}

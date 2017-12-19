package com.amazon.ionsql.errors

import com.amazon.ionsql.Base
import com.amazon.ionsql.IonSqlException
import com.amazon.ionsql.errors.Property.*
import org.junit.Test


class IonSqlExceptionTest : Base() {

    val errorMessage = "Error"
    val prefix = "${IonSqlException::class.qualifiedName}:"

    @Test
    fun errorMessageNoErrorCodeNoContext() {
        val ex = IonSqlException(errorMessage)
        assertEquals("$prefix $errorMessage\n\t<UNKNOWN>: at line <UNKNOWN>, column <UNKNOWN>: <UNKNOWN>\n", ex.toString())
    }

    @Test
    fun noErrorMessageErrorCodeContext() {
        val errorContext = PropertyValueMap()
        errorContext[COLUMN_NUMBER] = 10L
        errorContext[LINE_NUMBER] = 20L
        errorContext[TOKEN_STRING] = "c"

        val ex = IonSqlException(ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("$prefix Lexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }

    @Test
    fun customErrorMessageErrorCodeContext() {
        val errorContext = PropertyValueMap()
        errorContext[COLUMN_NUMBER] = 10L
        errorContext[LINE_NUMBER] = 20L
        errorContext[TOKEN_STRING] = "c"

        val ex = IonSqlException("Unexpected token", ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("$prefix Unexpected token\n\tLexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }

    @Test // IONSQL-180
    fun toStringDoesNotAccumulateMessageText() {
        val errorContext = PropertyValueMap()
        errorContext[COLUMN_NUMBER] = 10L
        errorContext[LINE_NUMBER] = 20L
        errorContext[TOKEN_STRING] = "c"

        val ex = IonSqlException("Unexpected token", ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("$prefix Unexpected token\n\tLexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
        assertEquals("$prefix Unexpected token\n\tLexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }
}
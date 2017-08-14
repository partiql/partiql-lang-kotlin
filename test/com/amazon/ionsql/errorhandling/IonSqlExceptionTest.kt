package com.amazon.ionsql.errorhandling

import com.amazon.ionsql.Base
import com.amazon.ionsql.IonSqlException
import org.junit.Test


class IonSqlExceptionTest : Base() {

    val errorMessage = "Error"

    @Test
    fun errorMessageNoErrorCodeNoContext(){
        val ex = IonSqlException(errorMessage)
        assertEquals("$errorMessage\n\t<UNKNOWN>: at line <UNKNOWN>, column <UNKNOWN>: <UNKNOWN>\n", ex.toString())
    }

    @Test
    fun noErrorMessageErrorCodeContext(){
        val errorContext = PropertyBag()
            .addProperty(Property.COLUMN_NO, 10L)
            .addProperty(Property.LINE_NO, 20L)
            .addProperty(Property.TOKEN_STRING, "c")

        val ex = IonSqlException(ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("Lexer error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }

    @Test
    fun customErrorMessageErrorCodeContext(){
        val errorContext = PropertyBag()
            .addProperty(Property.COLUMN_NO, 10L)
            .addProperty(Property.LINE_NO, 20L)
            .addProperty(Property.TOKEN_STRING, "c")

        val ex = IonSqlException("Unexpected token", ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("Unexpected token\n\tLexer error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }
}
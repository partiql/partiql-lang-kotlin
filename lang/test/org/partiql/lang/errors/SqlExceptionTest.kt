/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.errors

import org.partiql.lang.*
import org.partiql.lang.errors.Property.*
import org.junit.Test


class SqlExceptionTest : TestBase() {

    val errorMessage = "Error"
    val prefix = "${SqlException::class.qualifiedName}:"

//    @Test
//    fun errorMessageNoErrorCodeNoContext() {
//        val ex = SqlException(errorMessage)
//        assertEquals("$prefix $errorMessage\n\t<UNKNOWN>: at line <UNKNOWN>, column <UNKNOWN>: <UNKNOWN>\n", ex.toString())
//    }

    @Test
    fun noErrorMessageErrorCodeContext() {
        val errorContext = PropertyValueMap()
        errorContext[COLUMN_NUMBER] = 10L
        errorContext[LINE_NUMBER] = 20L
        errorContext[TOKEN_STRING] = "c"

        val ex = SqlException(ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("$prefix Lexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }

    @Test
    fun customErrorMessageErrorCodeContext() {
        val errorContext = PropertyValueMap()
        errorContext[COLUMN_NUMBER] = 10L
        errorContext[LINE_NUMBER] = 20L
        errorContext[TOKEN_STRING] = "c"

        val ex = SqlException("Unexpected token", ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("$prefix Unexpected token\n\tLexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }

    @Test
    fun toStringDoesNotAccumulateMessageText() {
        val errorContext = PropertyValueMap()
        errorContext[COLUMN_NUMBER] = 10L
        errorContext[LINE_NUMBER] = 20L
        errorContext[TOKEN_STRING] = "c"

        val ex = SqlException("Unexpected token", ErrorCode.LEXER_INVALID_CHAR, errorContext)

        assertEquals("$prefix Unexpected token\n\tLexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
        assertEquals("$prefix Unexpected token\n\tLexer Error: at line 20, column 10: invalid character at, c\n", ex.toString())
    }
}
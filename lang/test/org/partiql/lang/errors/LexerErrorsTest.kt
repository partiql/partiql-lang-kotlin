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

import org.junit.Test
import org.partiql.lang.TestBase
import org.partiql.lang.syntax.LexerException
import org.partiql.lang.syntax.SqlLexer
import org.partiql.lang.util.softAssert

class LexerErrorsTest : TestBase() {

    private val lexer = SqlLexer(ion)

    private fun representation(codePoint: Int): String =
        when {
            codePoint == -1 -> "<EOF>"
            codePoint < -1 -> "<$codePoint>"
            else -> "'${String(Character.toChars(codePoint))}' [U+${Integer.toHexString(codePoint)}]"
        }

    private fun checkInputThrowingLexerException(input: String,
                                                errorCode: ErrorCode,
                                                expectErrorContextValues: Map<Property, Any>) {
        try {
            lexer.tokenize(input)
            fail("Expected LexerException but there was no Exception")
        } catch (lex: LexerException) {
            softAssert {
                checkErrorAndErrorContext(errorCode, lex, expectErrorContextValues)
            }
        } catch (ex: Exception) {
            fail("Expected LexerException but a different exception was thrown \n\t  $ex")
        }
    }

    @Test
    fun testInvalidChar() {
        checkInputThrowingLexerException("©",
            ErrorCode.LEXER_INVALID_CHAR,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TOKEN_STRING to representation("©".codePointAt(0))))
    }

    @Test
    fun testInvalidOperator() {
        checkInputThrowingLexerException("10 ^ 4",
            ErrorCode.LEXER_INVALID_OPERATOR,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 5L,
                Property.TOKEN_STRING to "^"))
    }

    @Test
    fun testInvalidIonLiteral() {
        checkInputThrowingLexerException("`{I am not a list}`",
                                         ErrorCode.LEXER_INVALID_ION_LITERAL,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 20L,
                                             Property.TOKEN_STRING to "{I am not a list}"))
    }
 }

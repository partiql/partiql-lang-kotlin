/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

import com.amazon.ion.Timestamp
import org.junit.Test
import org.partiql.lang.syntax.PartiQLParserTestBase
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.parser.antlr.PartiQLParser

class ParserErrorsTest : PartiQLParserTestBase() {

    @Test
    fun emptyQuery() {
        checkInputThrowingParserException(
            "",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun expectedKeyword() {
        checkInputThrowingParserException(
            "5 BETWEEN 1  10",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(10)
            )
        )
    }

    @Test
    fun expectedTypeName() {
        checkInputThrowingParserException(
            "NULL is `null`",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ION_CLOSURE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newNull()
            )
        )
    }

    @Test
    fun expectedIdentAfterAT() {
        checkInputThrowingParserException(
            "@",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 2L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun expectedExpectedTypeName() {
        checkInputThrowingParserException(
            "a is 'missing'",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString("missing")
            )
        )
    }

    @Test
    fun expectedUnexpectedToken() {
        checkInputThrowingParserException(
            "SELECT ord, val FROM table1 AT ord AS val",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 36L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.AS.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("AS")
            )
        )
    }

    @Test
    fun expectedUnexpectedKeyword() {
        checkInputThrowingParserException(
            "SELECT FROM table1",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("FROM")
            )
        )
    }

    @Test
    fun unexpectedKeywordFromInSelectList() {
        checkInputThrowingParserException(
            "SELECT a, DATE '2012-12-12', FROM {'a' : 1}",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 30L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("FROM")
            )
        )
    }

    @Test
    fun unexpectedKeywordUpdateInSelectList() {
        checkInputThrowingParserException(
            "SELECT a, UPDATE FROM {'a' : 1}",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.UPDATE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("UPDATE")
            )
        )
    }

    @Test
    fun expectedInvalidPathComponent() {
        checkInputThrowingParserException(
            "x...a",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 3L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PERIOD.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(".")
            )
        )
    }

    @Test
    fun expectedInvalidPathComponentForKeyword() {
        checkInputThrowingParserException(
            """SELECT foo.id, foo.table FROM `[{id: 1, table: "foos"}]` AS foo""",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 20L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.TABLE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("table")
            )
        )
    }

    @Test
    fun expectedCastAsIntArity() {
        checkInputThrowingParserException(
            "CAST(5 AS INTEGER(10))",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            )
        )
    }

    @Test
    fun expectedCastAsRealArity() {
        checkInputThrowingParserException(
            "CAST(5 AS REAL(10))",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            )
        )
    }

    @Test
    fun expectedInvalidTypeParameter() {
        checkInputThrowingParserException(
            "CAST(5 AS VARCHAR(a))",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            )
        )
    }

    @Test
    fun castToVarCharToTooBigLength() {
        checkInputThrowingParserException(
            "CAST(5 AS VARCHAR(2147483648))",
            ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 19L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2147483648L)
            )
        )
    }

    @Test
    fun castToDecimalToTooBigLength_1() {
        checkInputThrowingParserException(
            "CAST(5 AS DECIMAL(2147483648))",
            ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 19L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2147483648L)
            )
        )
    }

    @Test
    fun castToDecimalToTooBigLength_2() {
        checkInputThrowingParserException(
            "CAST(5 AS DECIMAL(1, 2147483648))",
            ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 22L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2147483648L)
            )
        )
    }

    @Test
    fun castToNumericToTooBigLength_1() {
        checkInputThrowingParserException(
            "CAST(5 AS NUMERIC(2147483648))",
            ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 19L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2147483648L)
            )
        )
    }

    @Test
    fun castToNumericToTooBigLength_2() {
        checkInputThrowingParserException(
            "CAST(5 AS NUMERIC(1, 2147483648))",
            ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 22L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2147483648L)
            )
        )
    }

    @Test
    fun expectedExpectedWhenClause() {
        checkInputThrowingParserException(
            "CASE name ELSE 1 END",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ELSE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ELSE")
            )
        )
    }

    @Test
    fun expectedUnexpectedOperator() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE LIKE a b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 23L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("WHERE")
            )
        )
    }

    @Test
    fun expectedExpression() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE a LIKE b ESCAPE",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 44L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun expectedExpressionTernaryOperator() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE a LIKE",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 35L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun expectedTokenType() {
        checkInputThrowingParserException(
            "(1 + 2",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 7L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun expectedCastMissingLeftParen() {
        checkInputThrowingParserException(
            "CAST 5 as integer",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(5)
            )
        )
    }

    @Test
    fun expectedLeftParenValueConstructor() {
        checkInputThrowingParserException(
            "values 1,2)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(1)
            )
        )
    }

    @Test
    fun expectedUnexpectedTerm() {
        checkInputThrowingParserException(
            "select () from data",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            )
        )
    }

    @Test
    fun expectedSelectMissingFrom() {
        checkInputThrowingParserException(
            "select a  data",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun expectedUnsupportedLiteralsGroupBy() {
        checkInputThrowingParserException(
            "select a from data group by 1",
            ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(1)
            )
        )
    }

    @Test
    fun expectedAsForLet() {
        checkInputThrowingParserException(
            "SELECT a FROM foo LET bar b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 27L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("b")
            )
        )
    }

    @Test
    fun expectedIdentForAlias() {
        checkInputThrowingParserException(
            "select a as true from data",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 13L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.TRUE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newBool(true)
            )
        )
    }

    @Test
    fun expectedIdentForAt() {
        checkInputThrowingParserException(
            "select a from data at true",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 20L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.AT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("at")
            )
        )
    }

    @Test
    fun expectedIdentForAliasLet() {
        checkInputThrowingParserException(
            "SELECT a FROM foo LET bar AS",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun substringMissingLeftParen() {
        checkInputThrowingParserException(
            "select substring from 'asdf' for 1) FROM foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("from")
            )
        )
    }

    @Test
    fun substringMissingFromOrComma() {
        checkInputThrowingParserException(
            "select substring('str' 1) from foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 24L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(1)
            )
        )
    }

    @Test
    fun substringSql92WithoutLengthMissingRightParen() {
        checkInputThrowingParserException(
            "select substring('str' from 1 from foo ",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 31L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("from")
            )
        )
    }

    @Test
    fun substringSql92WithLengthMissingRightParen() {
        checkInputThrowingParserException(
            "select substring('str' from 1 for 1 from foo ",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 37L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("from")
            )
        )
    }

    @Test
    fun substringWithoutLengthMissingRightParen() {
        checkInputThrowingParserException(
            "select substring('str', 1 from foo ",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 27L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("from")
            )
        )
    }

    @Test
    fun substringMissingRightParen() {
        checkInputThrowingParserException(
            "select substring('str', 1, 1 from foo ",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 30L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("from")
            )
        )
    }

    @Test
    fun callTrimNoLeftParen() {
        checkInputThrowingParserException(
            "trim ' ')",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString(" ")
            )
        )
    }

    @Test
    fun callTrimNoRightParen() {
        checkInputThrowingParserException(
            "trim (' '",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun callTrimFourArguments() {
        checkInputThrowingParserException(
            "trim(both ' ' from 'test' 2)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 27L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2)
            )
        )
    }

    @Test
    fun callTrimSpecificationWithoutFrom() {
        checkInputThrowingParserException(
            "trim(both 'test')",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            )
        )
    }

    @Test
    fun callTrimSpecificationAndRemoveWithoutFrom() {
        checkInputThrowingParserException(
            "trim(both '' 'test')",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString("test")
            )
        )
    }

    @Test
    fun callTrimWithoutString() {
        checkInputThrowingParserException(
            "trim(from)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            )
        )
    }

    @Test
    fun callTrimNoArgs() {
        checkInputThrowingParserException(
            "trim()",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            )
        )
    }

    @Test
    fun callTrimSpecificationMissingFrom() {
        checkInputThrowingParserException(
            "trim(trailing '')",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),
        )
    }

    @Test
    fun callTrimZeroArguments() {
        checkInputThrowingParserException(
            "trim()",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),
        )
    }

    @Test
    fun callTrimAllButString() {
        checkInputThrowingParserException(
            "trim(trailing '' from)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 22L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),
        )
    }

    @Test
    fun callTwoArgumentsNoFrom() {
        checkInputThrowingParserException(
            "trim(' ' '   1   ')",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString("   1   ")
            ),
        )
    }

    @Test
    fun callTrimSpecificationAndFromMissingString() {
        checkInputThrowingParserException(
            "trim(trailing from)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 19L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),
        )
    }

    @Test
    fun callTrimSpecificationMismatch() {
        checkInputThrowingParserException(
            "trim(something ' ' from ' string ')",
            ErrorCode.PARSE_INVALID_TRIM_SPEC,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("something")
            ),
        )
    }

    @Test
    fun nullIsNotNullIonLiteral() {
        checkInputThrowingParserException(
            "NULL is not `null`",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 13L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ION_CLOSURE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newNull()
            ),
        )
    }

    @Test
    fun idIsNotStringLiteral() {
        checkInputThrowingParserException(
            "a is not 'missing'",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString("missing")
            ),
        )
    }

    @Test
    fun idIsNotGroupMissing() {
        checkInputThrowingParserException(
            "a is not (missing)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),
        )
    }

    @Test
    fun aggregateWithNoArgs() {
        checkInputThrowingParserException(
            "SUM()",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 5L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),
        )
    }

    @Test
    fun aggregateWithTooManyArgs() {
        checkInputThrowingParserException(
            "SUM(a, b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),
        )
    }

    @Test
    fun aggregateWithWildcardOnNonCount() {
        checkInputThrowingParserException(
            "SUM(*)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 5L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),
        )
    }

    @Test
    fun aggregateWithWildcardOnNonCountNonAggregate() {
        checkInputThrowingParserException(
            "F(*)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 2L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),
        )
    }

    @Test
    fun castTooManyArgs() {
        checkInputThrowingParserException(
            "CAST(5 AS INTEGER(10))",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),
        )
    }

    @Test
    fun castNonLiteralArg() {
        checkInputThrowingParserException(
            "CAST(5 AS VARCHAR(a))",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),
        )
    }

    @Test
    fun castNegativeArg() {
        checkInputThrowingParserException(
            "CAST(5 AS VARCHAR(-1))",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),
        )
    }

    @Test
    fun castNonTypArg() {
        checkInputThrowingParserException(
            "CAST(5 AS SELECT)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.SELECT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SELECT")
            ),
        )
    }

    @Test
    fun caseOnlyEnd() {
        checkInputThrowingParserException(
            "CASE END",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.END.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("END")
            ),
        )
    }

    @Test
    fun searchedCaseNoWhenWithElse() {
        checkInputThrowingParserException(
            "CASE ELSE 1 END",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ELSE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ELSE")
            ),
        )
    }

    @Test
    fun simpleCaseNoWhenWithElse() {
        checkInputThrowingParserException(
            "CASE name ELSE 1 END",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ELSE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ELSE")
            ),
        )
    }

    @Test
    fun groupByOrdinal() {
        checkInputThrowingParserException(
            "SELECT a FROM data GROUP BY 1",
            ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(1)
            )
        )
    }

    @Test
    fun groupByOutOfBoundsOrdinal() { // looks the same as the previous one
        checkInputThrowingParserException(
            "SELECT a FROM data GROUP BY 2",
            ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(2)
            )
        )
    }

    @Test
    fun groupByBadOrdinal() {
        checkInputThrowingParserException(
            "SELECT a FROM data GROUP BY -1", // looks duplicate
            ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.MINUS.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("-")
            ),
        )
    }

    @Test
    fun groupByStringConstantOrdinal() {
        checkInputThrowingParserException(
            "SELECT a FROM data GROUP BY 'a'",
            ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString("a")
            )
        )
    }

    // Some of the ORDER BY related tests are changed
    // because the window function adds an additional rule that uses ORDER BY.
    @Test
    fun orderByMissingByAndSortSpec() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun orderByMissingBy() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun orderByMissingSortSpec() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun orderByMultipleAttributesInSortSpec() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY foo bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 31L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("bar")
            )
        )
    }

    @Test
    fun orderByMultipleEmptyParsedCommaList() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY foo, ,",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 30L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),
        )
    }

    @Test
    fun orderByMissingAttributeName() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY asc, bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun orderByInvalidPunctuation() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY asc; bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun orderByMultipleOrderingSpecs() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY foo asc desc",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 35L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DESC.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("desc")
            )
        )
    }

    @Test
    fun orderByUnexpectedKeywordAsAttribute() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY SELECT",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun orderByMissingNullsType() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY a ASC NULLS",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 38L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            )
        )
    }

    @Test
    fun orderByMissingNullsKeywordWithFirstNullsType() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY a ASC FIRST",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 33L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FIRST.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("FIRST")
            )
        )
    }

    @Test
    fun orderByMissingNullsKeywordWithLastNullsType() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY a ASC LAST",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 33L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LAST.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("LAST")
            )
        )
    }

    @Test
    fun nullsBeforeOrderBy() {
        checkInputThrowingParserException(
            "SELECT a FROM tb NULLS LAST ORDER BY a ASC",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.NULLS.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("nulls")
            )
        )
    }

    @Test
    fun orderByUnexpectedNullsKeywordAsAttribute() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY a NULLS SELECT",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 35L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.SELECT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SELECT")
            )
        )
    }

    @Test
    fun orderByUnexpectedKeyword() {
        checkInputThrowingParserException(
            "SELECT a FROM tb ORDER BY a NULLS FIRST SELECT",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 41L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.SELECT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SELECT")
            )
        )
    }

    @Test
    fun offsetBeforeLimit() {
        checkInputThrowingParserException(
            "SELECT a FROM tb OFFSET 5 LIMIT 10",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 27L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LIMIT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("LIMIT")
            )
        )
    }

    @Test
    fun limitOffsetBeforeOrderBy() {
        checkInputThrowingParserException(
            "SELECT a FROM tb LIMIT 10 OFFSET 5 ORDER BY b ASC",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 36L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun limitOffsetBeforeOrderByWithNulls() {
        checkInputThrowingParserException(
            "SELECT a FROM tb LIMIT 10 OFFSET 5 ORDER BY b ASC NULLS FIRST",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 36L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ORDER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ORDER")
            )
        )
    }

    @Test
    fun offsetMissingArgument() {
        checkInputThrowingParserException(
            "SELECT a FROM tb OFFSET",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 24L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),
        )
    }

    @Test
    fun offsetUnexpectedKeywordAsAttribute() {
        checkInputThrowingParserException(
            "SELECT a FROM tb OFFSET SELECT",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 31L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),
        )
    }

    @Test
    fun onConflictUnexpectedTokenOnConflict() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 ON_CONFLICT WHERE bar DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 25L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ON_CONFLICT")
            )
        )
    }

    @Test
    fun onConflictUnexpectedKeywordConflict() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 CONFLICT WHERE bar DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 25L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.CONFLICT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("CONFLICT")
            )
        )
    }

    @Test
    fun onConflictUnexpectedKeywordWhen() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 ON CONFLICT WHEN bar DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 37L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.WHEN.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("WHEN")
            ),
        )
    }

    @Test
    fun onConflictMissingOnConflictExpression() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 ON CONFLICT WHERE DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 43L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DO.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("DO")
            ),
        )
    }

    @Test
    fun onConflictMissingConflictAction() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 46L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),
        )
    }

    @Test
    fun onConflictInvalidConflictAction() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar DO SOMETHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 50L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SOMETHING")
            ),
        )
    }

    @Test
    fun atOnConflictUnexpectedTokenOnConflict() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 AT pos ON_CONFLICT WHERE bar DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 32L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ON_CONFLICT")
            )
        )
    }

    @Test
    fun atOnConflictUnexpectedKeywordConflict() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 AT pos CONFLICT WHERE bar DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 32L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.CONFLICT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("CONFLICT")
            )
        )
    }

    @Test
    fun atOnConflictUnexpectedKeywordWhen() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHEN bar DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 44L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.WHEN.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("WHEN")
            ),
        )
    }

    @Test
    fun atOnConflictMissingOnConflictExpression() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHERE DO NOTHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 50L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DO.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("DO")
            ),
        )
    }

    @Test
    fun atOnConflictMissingConflictAction() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHERE bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 53L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),
        )
    }

    @Test
    fun atOnConflictInvalidConflictAction() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHERE bar DO SOMETHING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 57L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SOMETHING")
            ),
        )
    }

    @Test
    fun leftOvers() {
        checkInputThrowingParserException(
            "5 5",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 3L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(5)
            )
        )
    }

    @Test
    fun likeColNameLikeColNameEscapeTypo() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE a LIKE b ECSAPE '\\'",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 38L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ECSAPE")
            )
        )
    }

    // TODO: PartiQL Parser says the error location is `where` when it should be `like`
    //  See: https://github.com/partiql/partiql-lang-kotlin/issues/731
    @Test
    fun likeWrongOrderOfArgs() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE LIKE a b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 23L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("WHERE")
            ),
        )
    }

    @Test
    fun likeMissingEscapeValue() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE a LIKE b ESCAPE",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 44L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),
        )
    }

    @Test
    fun likeMissingPattern() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE a LIKE",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 35L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),
        )
    }

    @Test
    fun likeEscapeIncorrectOrder() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE ESCAPE '\\' a LIKE b ",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 23L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.WHERE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("WHERE")
            ),
        )
    }

    @Test
    fun likeEscapeAsSecondArgument() {
        checkInputThrowingParserException(
            "SELECT a, b FROM data WHERE a LIKE ESCAPE '\\' b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 36L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ESCAPE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("ESCAPE")
            ),
        )
    }

    @Test
    fun atOperatorOnNonIdentifier() {
        checkInputThrowingParserException(
            "@(a)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 2L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),
        )
    }

    @Test
    fun atOperatorDoubleOnIdentifier() {
        checkInputThrowingParserException(
            "@ @a",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 3L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.AT_SIGN.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("@")
            ),
        )
    }

    @Test
    fun nullIsNullIonLiteral() {
        checkInputThrowingParserException(
            "NULL is `null`",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ION_CLOSURE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newNull()
            ),

        )
    }

    @Test
    fun idIsStringLiteral() {
        checkInputThrowingParserException(
            "a is 'missing'",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newString("missing")
            ),

        )
    }

    @Test
    fun idIsGroupMissing() {
        checkInputThrowingParserException(
            "a is (missing)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("(")
            ),

        )
    }

    @Test
    fun selectWithFromAtAndAs() {
        checkInputThrowingParserException(
            "SELECT ord, val FROM table1 AT ord AS val",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 36L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.AS.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("AS")
            )
        )
    }

    @Test
    fun pivotNoAt() {
        checkInputThrowingParserException(
            "PIVOT v FROM data",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("FROM")
            ),

        )
    }

    @Test
    fun callExtractMissingFrom() {
        checkInputThrowingParserException(
            "extract(year b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("b")
            ),

        )
    }

    @Test
    fun callExtractMissingFromWithComma() {
        checkInputThrowingParserException(
            "extract(year, b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 13L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun callExtractMissingSecondArgument() {
        checkInputThrowingParserException(
            "extract(year from)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),

        )
    }

    @Test
    fun callExtractMissingDateTimePart() {
        checkInputThrowingParserException(
            "extract(from b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.FROM.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("from")
            ),

        )
    }

    @Test
    fun callExtractOnlySecondArgument() {
        checkInputThrowingParserException(
            "extract(b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),

        )
    }

    @Test
    fun callExtractWrongDateTime() {
        checkInputThrowingParserException(
            "extract(b from c)",
            ErrorCode.PARSE_EXPECTED_DATE_TIME_PART,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("b")
            )
        )
    }

    @Test
    fun callExtractOnlyDateTimePart() {
        checkInputThrowingParserException(
            "extract(year)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 13L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),

        )
    }

    // NOTE that we do not test DATE_DIFF below because the parser uses the same code for both date_add and date_diff

    @Test
    fun callDateAddNoArguments() {
        checkInputThrowingParserException(
            "date_add()",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),

        )
    }

    @Test
    fun callDateAddInvalidDateTimePart() {
        checkInputThrowingParserException(
            "date_add(foobar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 16L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun callDateAddOneArgument() {
        checkInputThrowingParserException(
            "date_add(year)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")"),
            ),

        )
    }

    @Test
    fun callDateAddOneArgumentTrailingComma() {
        checkInputThrowingParserException(
            "date_add(year,)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")")
            ),

        )
    }

    @Test
    fun callDateAddTwoArguments() {
        checkInputThrowingParserException(
            "date_add(year, b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(")"),
            ),

        )
    }
    @Test
    fun callDateAddCommaAfterThirdArgument() {
        checkInputThrowingParserException(
            "date_add(year, b, c,)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 20L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(","),
            ),

        )
    }

    @Test
    fun callDateAddMissingComma() {
        checkInputThrowingParserException(
            "date_add(year a, b)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("a"),
            ),

        )
    }

    @Test
    fun callDateAddMissingDateTimePart() {
        checkInputThrowingParserException(
            "date_add(a, b, c)",
            ErrorCode.PARSE_EXPECTED_DATE_TIME_PART,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("a")
            )
        )
    }

    @Test
    fun tokensAfterSemicolon() {
        checkInputThrowingParserException(
            "1;1",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 3L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(1)
            )
        )
    }

    @Test
    fun validQueriesSeparatedBySemicolon() {
        checkInputThrowingParserException(
            "SELECT * FROM <<1>>;SELECT * FROM <<1>>",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.SELECT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SELECT")
            )
        )
    }

    @Test
    fun semicolonInsideExpression() {
        checkInputThrowingParserException(
            "(1;)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 3L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COLON_SEMI.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(";")
            ),

        )
    }

    @Test
    fun selectStarStar() {
        checkInputThrowingParserException(
            "SELECT *, * FROM <<1>>",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun selectStarAliasDotStar() {
        checkInputThrowingParserException(
            "SELECT *, foo.* FROM <<{ a: 1 }>> as foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun selectAliasDotStarStar() {
        checkInputThrowingParserException(
            "SELECT foo.*, * FROM <<{ a: 1 }>> as foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun selectExpressionStar() {
        checkInputThrowingParserException(
            "SELECT 1, * FROM <<{ a: 1 }>>",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun selectStarExpression() {
        checkInputThrowingParserException(
            "SELECT *, 1 FROM <<{ a: 1 }>>",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun countDistinctStar() {
        checkInputThrowingParserException(
            "COUNT(DISTINCT *)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 16L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun countAllStar() {
        checkInputThrowingParserException(
            "COUNT(ALL *)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun countExpressionStar() {
        checkInputThrowingParserException(
            "COUNT(a, *)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun setWithNoAssignments() {
        checkInputThrowingParserException(
            "FROM x SET",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun setWithExpression() {
        checkInputThrowingParserException(
            "FROM x SET y, z",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 13L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun setWithWildcardPath() {
        checkInputThrowingParserException(
            "FROM x SET y.* = 5",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun setWithExpressionPath() {
        checkInputThrowingParserException(
            "FROM x SET y[1+1] = 5",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PLUS.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("+")
            ),

        )
    }

    @Test
    fun fromWithDelete() {
        checkInputThrowingParserException(
            "FROM x DELETE FROM y",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DELETE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("DELETE")
            ),

        )
    }

    @Test
    fun fromWithUpdate() {
        checkInputThrowingParserException(
            "FROM x UPDATE y SET a = b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.UPDATE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("UPDATE")
            ),

        )
    }

    @Test
    fun deleteNoFrom() = checkInputThrowingParserException(
        "DELETE x",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("x")
        )
    )

    @Test
    fun deleteFromList() = checkInputThrowingParserException(
        "DELETE FROM x, y",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol(",")
        )
    )

    @Test
    fun deleteFromListWithAListMemberThatHasPath() = checkInputThrowingParserException(
        "DELETE FROM x.n, a",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 16L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol(",")
        )
    )

    @Test
    fun deleteFromListWithAListMemberThatHasAnAlias() = checkInputThrowingParserException(
        "DELETE FROM x.n.m AS y, a",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 23L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol(",")
        )
    )

    @Test
    fun deleteFromListWithAListMemberThatHasAnAliasAndPosition() = checkInputThrowingParserException(
        "DELETE FROM x.n.m AS y AT z, a",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 28L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol(",")
        )
    )

    @Test
    fun updateNoSet() {
        checkInputThrowingParserException(
            "UPDATE x",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun updateWithNestedSet() {
        checkInputThrowingParserException(
            "UPDATE test SET x = SET test.y = 6",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.SET.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("SET")
            ),

        )
    }

    @Test
    fun updateWithRemove() {
        checkInputThrowingParserException(
            "UPDATE test SET x = REMOVE y",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.REMOVE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("REMOVE")
            ),

        )
    }

    @Test
    fun updateWithInsert() {
        checkInputThrowingParserException(
            "UPDATE test SET x = INSERT INTO foo VALUE 1",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.INSERT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("INSERT")
            ),

        )
    }

    @Test
    fun updateWithDelete() {
        checkInputThrowingParserException(
            "UPDATE test SET x = DELETE FROM y",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DELETE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("DELETE")
            ),

        )
    }

    @Test
    fun updateWithExec() {
        checkInputThrowingParserException(
            "UPDATE test SET x = EXEC foo arg1, arg2",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EXEC.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EXEC")
            ),

        )
    }

    @Test
    fun updateWithCreateTable() {
        checkInputThrowingParserException(
            "UPDATE test SET x = CREATE TABLE foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.CREATE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("CREATE")
            ),

        )
    }

    @Test
    fun updateWithDropTable() {
        checkInputThrowingParserException(
            "UPDATE test SET x = DROP TABLE foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DROP.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("DROP")
            ),

        )
    }

    @Test
    fun updateWithCreateIndex() {
        checkInputThrowingParserException(
            "UPDATE test SET x = CREATE INDEX ON foo (x, y.z)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.CREATE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("CREATE")
            ),

        )
    }

    @Test
    fun nestedRemove() {
        checkInputThrowingParserException(
            "REMOVE REMOVE y",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.REMOVE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("REMOVE")
            ),

        )
    }

    @Test
    fun nestedInsertInto() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE INSERT INTO foo VALUE 1 AT bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 23L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.INSERT.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("INSERT")
            ),

        )
    }

    @Test
    fun selectAndRemove() {
        checkInputThrowingParserException(
            "SELECT REMOVE foo FROM bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.REMOVE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("REMOVE")
            ),
        )
    }

    @Test
    fun selectAndRemove2() {
        checkInputThrowingParserException(
            "SELECT * FROM REMOVE foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.REMOVE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("REMOVE")
            ),

        )
    }

    @Test
    fun updateWithDropIndex() {
        checkInputThrowingParserException(
            "UPDATE test SET x = DROP INDEX bar ON foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.DROP.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("DROP")
            ),

        )
    }

    @Test
    fun updateFromList() {
        checkInputThrowingParserException(
            "UPDATE x, y SET a = b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun insertValueMissingReturning() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 MODIFIED OLD foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 25L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.MODIFIED.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("MODIFIED")
            ),
        )
    }

    @Test
    fun insertValueReturningMissingReturningElem() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 34L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun insertValueReturningMissingReturningMapping() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING *",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 35L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun insertValueReturningMissingReturningColumn() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 47L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun insertValueMultiReturningMissingReturningColumn() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD , ALL OLD *",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 48L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun insertValueMisSpellReturning() = checkInputThrowingParserException(
        "INSERT INTO foo VALUE 1 RETURING MODIFIED OLD foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 25L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("RETURING")
        )
    )

    @Test
    fun insertValueReturningInvalidReturningMapping() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING UPDATED OLD foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 35L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("UPDATED")
            ),

        )
    }

    @Test
    fun insertValueReturningInvalidReturningColumn() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD ;",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 48L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COLON_SEMI.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(";")
            ),

        )
    }

    @Test
    fun insertValueReturningMultipleReturningColumn() {
        checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD a,b",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 50L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("b")
            ),

        )
    }

    @Test
    fun createTableWithKeyword() = checkInputThrowingParserException(
        "CREATE TABLE SELECT",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.SELECT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("SELECT")
        )
    )

    @Test
    fun createForUnsupportedObject() = checkInputThrowingParserException(
        "CREATE VIEW FOO",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.VIEW.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("VIEW")
        )
    )

    @Test
    fun createTableWithNoIdentifier() = checkInputThrowingParserException(
        "CREATE TABLE",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 13L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("EOF")
        )
    )

    @Test
    fun createTableWithOperatorAfterIdentifier() = checkInputThrowingParserException(
        "CREATE TABLE foo-bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 17L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.MINUS.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("-")
        )
    )

    @Test
    fun nestedCreateTable() = checkInputThrowingParserException(
        "CREATE TABLE CREATE TABLE foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.CREATE.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("CREATE")
        )
    )

    @Test
    fun createTableWithoutColumns() = checkInputThrowingParserException(
        "CREATE TABLE Customer ()",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 24L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_RIGHT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol(")")
        )
    )

    @Test
    fun createTableNoCONSTRAINT() = checkInputThrowingParserException(
        "CREATE TABLE Customer (name string name_is_present NOT NULL)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 36L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("name_is_present")
        )
    )

    @Test
    fun createTableNoConstraintName() = checkInputThrowingParserException(
        "CREATE TABLE Customer (name string CONSTRAINT NOT NULL)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 47L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.NOT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("NOT")
        )
    )

    @Test
    fun createTableNoComma() = checkInputThrowingParserException(
        """
            CREATE TABLE Customer (
               age int
               city string NULL       
            )
        """.trimMargin(),
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 3L,
            Property.COLUMN_NUMBER to 16L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("city")
        )
    )

    @Test
    fun createTableNoColumnType() = checkInputThrowingParserException(
        "CREATE TABLE Customer (name NOT NULL)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 29L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.NOT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("NOT")
        )
    )

    @Test
    fun dropTableWithOperatorAfterIdentifier() = checkInputThrowingParserException(
        "DROP TABLE foo+bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 15L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PLUS.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("+")
        )
    )

    @Test
    fun createIndexWithoutAnythingElse() = checkInputThrowingParserException(
        "CREATE INDEX",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 13L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("EOF")
        )
    )

    @Test
    fun createIndexWithName() = checkInputThrowingParserException(
        "CREATE INDEX foo_index ON foo (bar)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("foo_index")
        )
    )

    @Test
    fun createIndexNoNameNoTarget() = checkInputThrowingParserException(
        "CREATE INDEX ON (bar)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 17L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("(")
        )
    )

    @Test
    fun createIndexNoNameNoKeyParenthesis() = checkInputThrowingParserException(
        "CREATE INDEX ON foo bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("bar")
        )
    )

    @Test
    fun createIndexNoNameKeyExpression() {
        checkInputThrowingParserException(
            "CREATE INDEX ON foo (1+1)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 22L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newInt(1)
            ),

        )
    }

    @Test
    fun createIndexWithOperatorAtTail() = checkInputThrowingParserException(
        "CREATE INDEX ON foo (bar) + 1",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 27L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PLUS.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("+")
        )
    )

    @Test
    fun createIndexNoNameKeyWildcardPath() {
        checkInputThrowingParserException(
            "CREATE INDEX ON foo (a.*)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 24L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.ASTERISK.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("*")
            ),

        )
    }

    @Test
    fun createIndexNoNameKeyExpressionPath() {
        checkInputThrowingParserException(
            "CREATE INDEX ON foo (a[1+1])",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 25L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.PLUS.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("+")
            ),

        )
    }

    @Test
    fun dropIndexWithoutAnythingElse() = checkInputThrowingParserException(
        "DROP INDEX",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 11L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("EOF")
        )
    )

    @Test
    fun dropIndexNoIdentifierNoTarget() = checkInputThrowingParserException(
        "DROP INDEX ON",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 12L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.ON.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("ON")
        )
    )

    @Test
    fun dropIndexMissingOnKeyWord() = checkInputThrowingParserException(
        "DROP INDEX bar foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 16L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("foo")
        )
    )

    @Test
    fun dropIndexWithExpression() = checkInputThrowingParserException(
        "DROP INDEX (1+1) on foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 12L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("(")
        )
    )

    @Test
    fun dropIndexWithParenthesisAtTail() = checkInputThrowingParserException(
        "DROP INDEX goo ON foo (bar)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 23L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("(")
        )
    )

    @Test
    fun dropIndexWithOperatorAtTail() = checkInputThrowingParserException(
        "DROP INDEX bar ON foo + 1",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 23L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.PLUS.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("+")
        )
    )

    @Test
    fun insertValueWithCollection() = checkInputThrowingParserException(
        "INSERT INTO foo VALUE spam, eggs",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 27L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol(",")
        )
    )

    @Test
    fun insertValuesWithAt() = checkInputThrowingParserException(
        "INSERT INTO foo VALUES (1, 2) AT bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 31L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.AT.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("AT")
        )
    )

    @Test
    fun valueAsTopLevelExpression() {
        checkInputThrowingParserException(
            "VALUE 1",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.VALUE.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("VALUE")
            ),

        )
    }

    @Test
    fun innerCrossJoinWithOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo INNER CROSS JOIN bar ON true",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 40L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.ON.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("ON")
        )
    )

    @Test
    fun leftCrossJoinWithOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo LEFT CROSS JOIN bar ON true",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 39L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.ON.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("ON")
        )
    )

    @Test
    fun rightCrossJoinWithOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo RIGHT CROSS JOIN bar ON true",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 40L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.ON.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("ON")
        )
    )

    @Test
    fun innerJoinWithOutOnCondition() {
        checkInputThrowingParserException(
            "SELECT * FROM foo INNER JOIN bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 33L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun leftJoinWithOutOnCondition() {
        checkInputThrowingParserException(
            "SELECT * FROM foo LEFT JOIN bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 32L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun rightJoinWithOutOnCondition() {
        checkInputThrowingParserException(
            "SELECT * FROM foo RIGHT JOIN bar",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 33L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun parenJoinWithoutOnClause() {
        checkInputThrowingParserException(
            "SELECT * FROM foo INNER JOIN (bar INNER JOIN baz ON true)",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 58L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    // ****************************************
    // EXEC clause parsing errors
    // ****************************************

    @Test
    fun execNoStoredProcedureProvided() {
        checkInputThrowingParserException(
            "EXEC",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 5L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun execCommaBetweenStoredProcedureAndArg() {
        checkInputThrowingParserException(
            "EXEC foo, arg0, arg1",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.COMMA.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol(",")
            ),

        )
    }

    @Test
    fun execArgTrailingComma() {
        checkInputThrowingParserException(
            "EXEC foo arg0, arg1,",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 21L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EOF")
            ),

        )
    }

    @Test
    fun execUnexpectedParen() {
        checkInputThrowingParserException(
            "EXEC foo()",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("foo")
            ),
        )
    }

    @Test
    fun execAtUnexpectedLocation() {
        checkInputThrowingParserException(
            "EXEC EXEC",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EXEC.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EXEC")
            ),

        )
    }

    @Test
    fun execAtUnexpectedLocationAfterExec() {
        checkInputThrowingParserException(
            "EXEC foo EXEC",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_DESCRIPTION to PartiQLParser.EXEC.getAntlrDisplayString(),
                Property.TOKEN_VALUE to ion.newSymbol("EXEC")
            ),

        )
    }

    @Test
    fun missingDateString() = checkInputThrowingParserException(
        "DATE",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 5L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newSymbol("EOF")
        )
    )

    @Test
    fun invalidTypeIntForDateString() = checkInputThrowingParserException(
        "DATE 2012",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newInt(2012)
        )
    )

    @Test
    fun invalidTypeIntForDateString2() = checkInputThrowingParserException(
        "DATE 2012-08-28",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_INTEGER.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newInt(2012)
        )
    )

    @Test
    fun invalidTypeTimestampForDateString() = checkInputThrowingParserException(
        "DATE `2012-08-28`",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.ION_CLOSURE.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newTimestamp(Timestamp.forDay(2012, 8, 28))
        )
    )

    @Test
    fun invalidDateStringFormat() = checkInputThrowingParserException(
        "DATE 'date_string'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("date_string")
        )
    )

    @Test
    fun invalidDateStringFormatMissingDashes() = checkInputThrowingParserException(
        "DATE '20210310'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("20210310")
        )
    )

    @Test
    fun invalidDateStringFormatUnexpectedColons() = checkInputThrowingParserException(
        "DATE '2021:03:10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("2021:03:10")
        )
    )

    @Test
    fun invalidDateStringFormatInvalidDate() = checkInputThrowingParserException(
        "DATE '2021-02-29'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("2021-02-29")
        )
    )

    @Test
    fun invalidDateStringFormatMMDDYYYY() = checkInputThrowingParserException(
        "DATE '03-10-2021'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("03-10-2021")
        )
    )

    @Test
    fun invalidDateStringFormatDDMMYYYY() = checkInputThrowingParserException(
        "DATE '10-03-2021'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("10-03-2021")
        )
    )

    @Test
    fun invalidExtendedDateString() = checkInputThrowingParserException(
        "DATE '+99999-03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("+99999-03-10")
        )
    )

    @Test
    fun invalidDateStringNegativeYear() = checkInputThrowingParserException(
        "DATE '-9999-03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("-9999-03-10")
        )
    )

    @Test
    fun invalidDateStringPositiveYear() = checkInputThrowingParserException(
        "DATE '+9999-03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("+9999-03-10")
        )
    )

    @Test
    fun invalidDateStringNegativeMonth() = checkInputThrowingParserException(
        "DATE '2021--03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("2021--03-10")
        )
    )

    @Test
    fun invalidDateStringPositiveMonth() = checkInputThrowingParserException(
        "DATE '2021-+03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("2021-+03-10")
        )
    )

    @Test
    fun invalidDateStringNegativeDay() = checkInputThrowingParserException(
        "DATE '2021-03--10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("2021-03--10")
        )
    )

    @Test
    fun invalidDateStringPositiveDay() = checkInputThrowingParserException(
        "DATE '2021-03-+10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("2021-03-+10")
        )
    )

    @Test
    fun invalidDateStringMonthOutOfRange() = checkInputThrowingParserException(
        "DATE '9999-300000000-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("9999-300000000-10")
        )
    )

    @Test
    fun invalidDateStringDayOutOfRangeForOct() = checkInputThrowingParserException(
        "DATE '1999-10-32'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("1999-10-32")
        )
    )

    @Test
    fun invalidDateStringDayOutOfRangeForNov() = checkInputThrowingParserException(
        "DATE '1999-11-31'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("1999-11-31")
        )
    )

    @Test
    fun invalidDateStringDayPaddedZeroMissingFromMonth() = checkInputThrowingParserException(
        "DATE '1999-1-31'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_DESCRIPTION to PartiQLParser.LITERAL_STRING.getAntlrDisplayString(),
            Property.TOKEN_VALUE to ion.newString("1999-1-31")
        )
    )
}

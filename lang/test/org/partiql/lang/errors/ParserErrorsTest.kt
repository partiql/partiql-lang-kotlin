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
import com.amazon.ion.Timestamp
import org.partiql.lang.syntax.TokenType
import org.partiql.lang.util.*
import org.junit.*
import org.partiql.lang.syntax.SqlParserTestBase

class ParserErrorsTest : SqlParserTestBase() {

    @Test
    fun expectedKeyword() {
        checkInputThrowingParserException("5 BETWEEN 1  10",
            ErrorCode.PARSE_EXPECTED_KEYWORD,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.KEYWORD to "AND",
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newInt(10)))
    }

    @Test
    fun expectedTypeName() {
        checkInputThrowingParserException("NULL is `null`",
            ErrorCode.PARSE_EXPECTED_TYPE_NAME,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_TYPE to TokenType.ION_LITERAL,
                Property.TOKEN_VALUE to ion.newNull()))

    }

    @Test
    fun expectedIdentAfterAT() {
        checkInputThrowingParserException("@",
            ErrorCode.PARSE_MISSING_IDENT_AFTER_AT,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 1L,
                Property.TOKEN_TYPE to TokenType.OPERATOR,
                Property.TOKEN_VALUE to ion.newSymbol("@")))

    }

    @Test
    fun expectedExpectedTypeName() {
        checkInputThrowingParserException("a is 'missing'",
            ErrorCode.PARSE_EXPECTED_TYPE_NAME,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newString("missing")))

    }

    @Test
    fun expectedUnexpectedToken() {
        checkInputThrowingParserException("SELECT ord, val FROM table1 AT ord AS val",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 36L,
                Property.TOKEN_TYPE to TokenType.AS,
                Property.TOKEN_VALUE to ion.newSymbol("as")))

    }

    // FIXME This is still an error--but an error in a different way
    @Ignore
    @Test
    fun expectedUnexpectedKeyword() {
        checkInputThrowingParserException("SELECT FROM table1",
            ErrorCode.PARSE_UNEXPECTED_KEYWORD,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("from")))

    }

    @Test
    fun expectedInvalidPathComponent() {
        checkInputThrowingParserException("x...a",
            ErrorCode.PARSE_INVALID_PATH_COMPONENT,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 3L,
                Property.TOKEN_TYPE to TokenType.DOT,
                Property.TOKEN_VALUE to ion.newSymbol(".")))

    }

    @Test
    fun expectedInvalidPathComponentForKeyword() {
        checkInputThrowingParserException("""SELECT foo.id, foo.table FROM `[{id: 1, table: "foos"}]` AS foo""",
                                          ErrorCode.PARSE_INVALID_PATH_COMPONENT,
                                          mapOf(
                                              Property.LINE_NUMBER to 1L,
                                              Property.COLUMN_NUMBER to 20L,
                                              Property.TOKEN_TYPE to TokenType.KEYWORD,
                                              Property.TOKEN_VALUE to ion.newSymbol("table")))

    }

    @Test
    fun expectedCastArity() {
        checkInputThrowingParserException("CAST(5 AS INTEGER(10))",
            ErrorCode.PARSE_CAST_ARITY,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 18L,
                Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
                Property.EXPECTED_ARITY_MIN to 0,
                Property.EXPECTED_ARITY_MAX to 0,
                Property.CAST_TO to "integer",
                Property.TOKEN_VALUE to ion.newSymbol("(")))

    }

    @Test
    fun expectedInvalidTypeParameter() {
        checkInputThrowingParserException("CAST(5 AS VARCHAR(a))",
            ErrorCode.PARSE_INVALID_TYPE_PARAM,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("character_varying")))

    }

    @Test
    fun expectedExpectedWhenClause() {
        checkInputThrowingParserException("CASE name ELSE 1 END",
            ErrorCode.PARSE_EXPECTED_WHEN_CLAUSE,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("else")))

    }

    @Test
    fun expectedUnexpectedOperator() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE LIKE a b",
            ErrorCode.PARSE_UNEXPECTED_OPERATOR,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_TYPE to TokenType.OPERATOR,
                Property.TOKEN_VALUE to ion.newSymbol("like")))

    }

    @Test
    fun expectedExpression() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE a LIKE b ESCAPE",
            ErrorCode.PARSE_EXPECTED_EXPRESSION,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 38L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("escape")))

    }

    @Test
    fun expectedExpressionTernaryOperator() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE a LIKE",
            ErrorCode.PARSE_EXPECTED_EXPRESSION,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 31L,
                Property.TOKEN_TYPE to TokenType.OPERATOR,
                Property.TOKEN_VALUE to ion.newSymbol("like")))

    }

    @Test
    fun expectedTokenType() {
        checkInputThrowingParserException("(1 + 2",
            ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
            mapOf(
                Property.EXPECTED_TOKEN_TYPE to TokenType.RIGHT_PAREN,
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 7L,
                Property.TOKEN_TYPE to TokenType.EOF,
                Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    }

    @Test
    fun expectedCastMissingLeftParen() {
        checkInputThrowingParserException("CAST 5 as integer",
            ErrorCode.PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 6L,
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newInt(5)))

    }

    @Test
    fun expectedLeftParenValueConstructor() {
        checkInputThrowingParserException("values 1,2)",
            ErrorCode.PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 8L,
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newInt(1)))

    }

    @Test
    fun expectedUnexpectedTerm() {
        checkInputThrowingParserException("select () from data",
            ErrorCode.PARSE_UNEXPECTED_TERM,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 9L,
                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol(")")))

    }

    @Test
    fun expectedSelectMissingFrom() {
        checkInputThrowingParserException("select a  data",
            ErrorCode.PARSE_SELECT_MISSING_FROM,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_TYPE to TokenType.EOF,
                Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    }

    @Test
    fun expectedUnsupportedLiteralsGroupBy() {
        checkInputThrowingParserException("select a from data group by 1",
                                         ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
                                         mapOf(Property.LINE_NUMBER to 1L,
                                               Property.COLUMN_NUMBER to 29L,
                                               Property.TOKEN_TYPE to TokenType.LITERAL,
                                               Property.TOKEN_VALUE to ion.newInt(1)))
    }

    @Test
    fun expectedAsForLet() {
        checkInputThrowingParserException("SELECT a FROM foo LET bar b",
            ErrorCode.PARSE_EXPECTED_AS_FOR_LET,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 27L,
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("b")))
    }

    @Test
    fun expectedIdentForAlias() {
        checkInputThrowingParserException("select a as true from data",
            ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 13L,
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newBool(true)))

    }

    @Test
    fun expectedIdentForAt() {
        checkInputThrowingParserException("select a from data at true",
            ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 23L,
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newBool(true)))

    }

    @Test
    fun expectedIdentForAliasLet() {
        checkInputThrowingParserException("SELECT a FROM foo LET bar AS",
            ErrorCode.PARSE_EXPECTED_IDENT_FOR_ALIAS,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 29L,
                Property.TOKEN_TYPE to TokenType.EOF,
                Property.TOKEN_VALUE to ion.newSymbol("EOF")))
    }

    @Test
    fun substringMissingLeftParen() {
                                        //12345678901234567890123456789
        checkInputThrowingParserException("select substring from 'asdf' for 1) FROM foo",
                ErrorCode.PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 18L,
                    Property.TOKEN_TYPE to TokenType.KEYWORD,
                    Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun substringMissingFromOrComma() {
                                        //12345678901234567890123456789
        checkInputThrowingParserException("select substring('str' 1) from foo",
                ErrorCode.PARSE_EXPECTED_ARGUMENT_DELIMITER,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 24L,
                    Property.TOKEN_TYPE to TokenType.LITERAL,
                    Property.TOKEN_VALUE to ion.newInt(1)))
    }

    @Test
    fun substringSql92WithoutLengthMissingRightParen() {
                                        //123456789012345678901234567890123456789
        checkInputThrowingParserException("select substring('str' from 1 from foo ",
                ErrorCode.PARSE_EXPECTED_2_TOKEN_TYPES,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.EXPECTED_TOKEN_TYPE_1_OF_2 to TokenType.FOR,
                    Property.EXPECTED_TOKEN_TYPE_2_OF_2 to TokenType.RIGHT_PAREN,
                    Property.COLUMN_NUMBER to 31L,
                    Property.TOKEN_TYPE to TokenType.KEYWORD,
                    Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun substringSql92WithLengthMissingRightParen() {
                                        //123456789012345678901234567890123456789
        checkInputThrowingParserException("select substring('str' from 1 for 1 from foo ",
                ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
                mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 37L,
                    Property.TOKEN_TYPE to TokenType.KEYWORD,
                    Property.EXPECTED_TOKEN_TYPE to TokenType.RIGHT_PAREN,
                    Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun substringWithoutLengthMissingRightParen() {
                                        //123456789012345678901234567890123456789
        checkInputThrowingParserException("select substring('str', 1 from foo ",
                ErrorCode.PARSE_EXPECTED_2_TOKEN_TYPES,
                mapOf(
                        Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 27L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.EXPECTED_TOKEN_TYPE_1_OF_2 to TokenType.COMMA,
                        Property.EXPECTED_TOKEN_TYPE_2_OF_2 to TokenType.RIGHT_PAREN,
                        Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun substringMissingRightParen() {
                                        //123456789012345678901234567890123456789
        checkInputThrowingParserException("select substring('str', 1, 1 from foo ",
                ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
                mapOf(
                        Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 30L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.EXPECTED_TOKEN_TYPE to TokenType.RIGHT_PAREN,
                        Property.TOKEN_VALUE to ion.newSymbol("from")))

    }

    @Test
    fun callTrimNoLeftParen() {
        checkInputThrowingParserException("trim ' ')",
                                         ErrorCode.PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 6L,
                                             Property.TOKEN_TYPE to TokenType.LITERAL,
                                             Property.TOKEN_VALUE to ion.newString(" ")))
    }

    @Test
    fun callTrimNoRightParen() {
        checkInputThrowingParserException("trim (' '",
                                         ErrorCode.PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 10L,
                                             Property.TOKEN_TYPE to TokenType.EOF,
                                             Property.TOKEN_VALUE to ion.newSymbol("EOF")))
    }

    @Test
    fun callTrimFourArguments() {
        checkInputThrowingParserException("trim(both ' ' from 'test' 2)",
                                         ErrorCode.PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 27L,
                                             Property.TOKEN_TYPE to TokenType.LITERAL,
                                             Property.TOKEN_VALUE to ion.newInt(2)))
    }

    @Test
    fun callTrimSpecificationWithoutFrom() {
        checkInputThrowingParserException("trim(both 'test')",
                                         ErrorCode.PARSE_EXPECTED_KEYWORD,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 17L,
                                             Property.KEYWORD to "FROM",
                                             Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                             Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callTrimSpecificationAndRemoveWithoutFrom() {
        checkInputThrowingParserException("trim(both '' 'test')",
                                         ErrorCode.PARSE_EXPECTED_KEYWORD,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 14L,
                                             Property.TOKEN_TYPE to TokenType.LITERAL,
                                             Property.KEYWORD to "FROM",
                                             Property.TOKEN_VALUE to ion.newString("test")))
    }

    @Test
    fun callTrimWithoutString() {
        checkInputThrowingParserException("trim(from)",
                                         ErrorCode.PARSE_UNEXPECTED_TERM,
                                         mapOf(
                                             Property.LINE_NUMBER to 1L,
                                             Property.COLUMN_NUMBER to 10L,
                                             Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                             Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callTrimNoArgs() {
        checkInputThrowingParserException("trim()",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(
                                              Property.LINE_NUMBER to 1L,
                                              Property.COLUMN_NUMBER to 6L,
                                              Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                              Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callTrimSpecificationMissingFrom() {
        checkInputThrowingParserException("trim(trailing '')",
                                          ErrorCode.PARSE_EXPECTED_KEYWORD,
                                          mapOf(
                                              Property.LINE_NUMBER to 1L,
                                              Property.COLUMN_NUMBER to 17L,
                                              Property.KEYWORD to "FROM",
                                              Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                              Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callTrimZeroArguments() {
        checkInputThrowingParserException("trim()",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 6L,
                                                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callTrimAllButString() {
        checkInputThrowingParserException("trim(trailing '' from)",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(
                                              Property.LINE_NUMBER to 1L,
                                              Property.COLUMN_NUMBER to 22L,
                                              Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                              Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callTwoArgumentsNoFrom() {
        checkInputThrowingParserException("trim(' ' '   1   ')",
                                          ErrorCode.PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL,
                                          mapOf(
                                              Property.LINE_NUMBER to 1L,
                                              Property.COLUMN_NUMBER to 10L,
                                              Property.TOKEN_TYPE to TokenType.LITERAL,
                                              Property.TOKEN_VALUE to ion.newString("   1   ")))
    }

    @Test
    fun callTrimSpecificationAndFromMissingString() {
        checkInputThrowingParserException("trim(trailing from)",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(
                                              Property.LINE_NUMBER to 1L,
                                              Property.COLUMN_NUMBER to 19L,
                                              Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                              Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun nullIsNotNullIonLiteral() {
        checkInputThrowingParserException("NULL is not `null`",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 13L,
                                                Property.TOKEN_TYPE to TokenType.ION_LITERAL,
                                                Property.TOKEN_VALUE to ion.newNull()))
    }

    @Test
    fun idIsNotStringLiteral() {
        checkInputThrowingParserException("a is not 'missing'",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 10L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newString("missing")))
    }

    @Test
    fun idIsNotGroupMissing() {
        checkInputThrowingParserException("a is not (missing)",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME ,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 10L,
                                                Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol("(")))
    }

    @Test
    fun aggregateWithNoArgs() {
        checkInputThrowingParserException("SUM()",
                                          ErrorCode.PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 5L,
                                                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun aggregateWithTooManyArgs() {
        checkInputThrowingParserException("SUM(a, b)",
                                          ErrorCode.PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 5L,
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.TOKEN_VALUE to ion.newSymbol("a")))
    }

    @Test
    fun aggregateWithWildcardOnNonCount() {
        checkInputThrowingParserException("SUM(*)",
                                          ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 5L,
                                                Property.TOKEN_TYPE to TokenType.STAR,
                                                Property.TOKEN_VALUE to ion.newSymbol("*")))
    }

    @Test
    fun aggregateWithWildcardOnNonCountNonAggregate() {
        checkInputThrowingParserException("F(*)",
                                          ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 3L,
                                                Property.TOKEN_TYPE to TokenType.STAR,
                                                Property.TOKEN_VALUE to ion.newSymbol("*")))
    }

    @Test
    fun castTooManyArgs() {
        checkInputThrowingParserException("CAST(5 AS INTEGER(10))",
                                          ErrorCode.PARSE_CAST_ARITY,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 18L,
                                                Property.EXPECTED_ARITY_MIN to 0, // kinda funny
                                                Property.EXPECTED_ARITY_MAX to 0,
                                                Property.CAST_TO to "integer",
                                                Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol("(")))
    }

    @Test
    fun castNonLiteralArg() {
        checkInputThrowingParserException("CAST(5 AS VARCHAR(a))",
                                          ErrorCode.PARSE_INVALID_TYPE_PARAM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 11L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("character_varying")))
    }

    @Test
    fun castNegativeArg() {
        checkInputThrowingParserException("CAST(5 AS VARCHAR(-1))",
                                          ErrorCode.PARSE_INVALID_TYPE_PARAM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 11L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("character_varying")))
    }

    @Test
    fun castNonTypArg() {
        checkInputThrowingParserException("CAST(5 AS SELECT)",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 11L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("select")))
    }

    @Test
    fun caseOnlyEnd() {
        checkInputThrowingParserException("CASE END",
                                          ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 6L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("end")))
    }

    @Test
    fun searchedCaseNoWhenWithElse() {
        checkInputThrowingParserException("CASE ELSE 1 END",
                                          ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 6L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("else")))
    }

    @Test
    fun simpleCaseNoWhenWithElse() {
        checkInputThrowingParserException("CASE name ELSE 1 END",
                                          ErrorCode.PARSE_EXPECTED_WHEN_CLAUSE,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 11L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("else")))
    }

    @Test
    fun groupByOrdinal() {
        checkInputThrowingParserException("SELECT a FROM data GROUP BY 1",
                                          ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 29L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newInt(1)))
    }

    @Test
    fun groupByOutOfBoundsOrdinal() { // looks the same as the previous one
        checkInputThrowingParserException("SELECT a FROM data GROUP BY 2",
                                          ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 29L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newInt(2)))
    }

    @Test
    fun groupByBadOrdinal() {
        checkInputThrowingParserException("SELECT a FROM data GROUP BY -1", // looks duplicate
                                          ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 30L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newInt(-1)))
    }

    @Test
    fun groupByStringConstantOrdinal() {
        checkInputThrowingParserException("SELECT a FROM data GROUP BY 'a'",
                                          ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 29L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newString("a")))
    }

    @Test
    fun orderByMissingBYAndSortSpec() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER",
                                          ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 23L,
                                                Property.TOKEN_TYPE to TokenType.EOF,
                                                Property.EXPECTED_TOKEN_TYPE to TokenType.BY,
                                                Property.TOKEN_VALUE to ion.newSymbol("EOF"))
        )
    }

    @Test
    fun orderByMissingBy() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER foo",
                                          ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 24L,
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.EXPECTED_TOKEN_TYPE to TokenType.BY,
                                                Property.TOKEN_VALUE to ion.newSymbol("foo"))
        )
    }

    @Test
    fun orderByMissingSortSpec() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 26L,
                                                Property.TOKEN_TYPE to TokenType.EOF,
                                                Property.TOKEN_VALUE to ion.newSymbol("EOF"))
        )
    }

    @Test
    fun orderByMultipleAttributesInSortSpec() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY foo bar",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 31L,
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.TOKEN_VALUE to ion.newSymbol("bar"))
        )
    }

    @Test
    fun orderByMultipleEmptyParsedCommaList() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY foo, ,",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 32L,
                                                Property.TOKEN_TYPE to TokenType.COMMA,
                                                Property.TOKEN_VALUE to ion.newSymbol(","))
        )
    }

    @Test
    fun orderByMissingAttributeName() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY asc, bar",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 27L,
                                                Property.TOKEN_TYPE to TokenType.ASC,
                                                Property.TOKEN_VALUE to ion.newSymbol("asc"))
        )
    }

    @Test
    fun orderByInvalidPunctuation() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY asc; bar",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 27L,
                                                Property.TOKEN_TYPE to TokenType.ASC,
                                                Property.TOKEN_VALUE to ion.newSymbol("asc"))
        )
    }

    @Test
    fun orderByMultipleOrderingSpecs() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY foo asc desc",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 35L,
                                                Property.TOKEN_TYPE to TokenType.DESC,
                                                Property.TOKEN_VALUE to ion.newSymbol("desc"))
        )
    }

    @Test
    fun orderByUnexpectedKeywordAsAttribute() {
        checkInputThrowingParserException("SELECT a FROM tb ORDER BY SELECT",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 33L,
                                                Property.TOKEN_TYPE to TokenType.EOF,
                                                Property.TOKEN_VALUE to ion.newSymbol("EOF"))
        )
    }

    @Test
    fun onConflictUnexpectedTokenOnConflict() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 ON_CONFLICT WHERE bar DO NOTHING",
                ErrorCode.PARSE_UNEXPECTED_TOKEN,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 25L,
                        Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                        Property.TOKEN_VALUE to ion.newSymbol("ON_CONFLICT"))
        )
    }

    @Test
    fun onConflictUnexpectedKeywordConflict() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 CONFLICT WHERE bar DO NOTHING",
                ErrorCode.PARSE_UNEXPECTED_TOKEN,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 25L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("conflict"))
        )
    }

    @Test
    fun onConflictUnexpectedKeywordWhen() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 ON CONFLICT WHEN bar DO NOTHING",
                ErrorCode.PARSE_EXPECTED_WHERE_CLAUSE,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 37L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("when"))
        )
    }

    @Test
    fun onConflictMissingOnConflictExpression() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 ON CONFLICT WHERE DO NOTHING",
                ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 43L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("do_nothing"))
        )
    }

    @Test
    fun onConflictMissingConflictAction() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar",
                ErrorCode.PARSE_EXPECTED_CONFLICT_ACTION,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 37L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("where"))
        )
    }

    @Test
    fun onConflictInvalidConflictAction() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 ON CONFLICT WHERE bar DO SOMETHING",
                ErrorCode.PARSE_EXPECTED_CONFLICT_ACTION,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 37L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("where"))
        )
    }

    @Test
    fun atOnConflictUnexpectedTokenOnConflict() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 AT pos ON_CONFLICT WHERE bar DO NOTHING",
                ErrorCode.PARSE_UNEXPECTED_TOKEN,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 32L,
                        Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                        Property.TOKEN_VALUE to ion.newSymbol("ON_CONFLICT"))
        )
    }

    @Test
    fun atOnConflictUnexpectedKeywordConflict() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 AT pos CONFLICT WHERE bar DO NOTHING",
                ErrorCode.PARSE_UNEXPECTED_TOKEN,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 32L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("conflict"))
        )
    }

    @Test
    fun atOnConflictUnexpectedKeywordWhen() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHEN bar DO NOTHING",
                ErrorCode.PARSE_EXPECTED_WHERE_CLAUSE,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 44L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("when"))
        )
    }

    @Test
    fun atOnConflictMissingOnConflictExpression() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHERE DO NOTHING",
                ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 50L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("do_nothing"))
        )
    }

    @Test
    fun atOnConflictMissingConflictAction() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHERE bar",
                ErrorCode.PARSE_EXPECTED_CONFLICT_ACTION,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 44L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("where"))
        )
    }

    @Test
    fun atOnConflictInvalidConflictAction() {
        checkInputThrowingParserException("INSERT INTO foo VALUE 1 AT pos ON CONFLICT WHERE bar DO SOMETHING",
                ErrorCode.PARSE_EXPECTED_CONFLICT_ACTION,
                mapOf(Property.LINE_NUMBER to 1L,
                        Property.COLUMN_NUMBER to 44L,
                        Property.TOKEN_TYPE to TokenType.KEYWORD,
                        Property.TOKEN_VALUE to ion.newSymbol("where"))
        )
    }

    @Test
    fun leftOvers() {
        checkInputThrowingParserException("5 5",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 3L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newInt(5)))
    }

    @Test
    fun likeColNameLikeColNameEscapeTypo() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE a LIKE b ECSAPE '\\'",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN ,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 38L,
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.TOKEN_VALUE to ion.newSymbol("ECSAPE")))
    }

    @Test
    fun likeWrongOrderOfArgs() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE LIKE a b",
                                          ErrorCode.PARSE_UNEXPECTED_OPERATOR,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 29L,
                                                Property.TOKEN_TYPE to TokenType.OPERATOR,
                                                Property.TOKEN_VALUE to ion.newSymbol("like")))
    }

    @Test
    fun likeMissingEscapeValue() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE a LIKE b ESCAPE",
                                          ErrorCode.PARSE_EXPECTED_EXPRESSION,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 38L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("escape")))
    }

    @Test
    fun likeMissingPattern() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE a LIKE",
                                          ErrorCode.PARSE_EXPECTED_EXPRESSION,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 31L,
                                                Property.TOKEN_TYPE to TokenType.OPERATOR,
                                                Property.TOKEN_VALUE to ion.newSymbol("like")))
    }

    @Test
    fun likeEscapeIncorrectOrder() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE ESCAPE '\\' a LIKE b ",
                                          ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 29L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("escape")))
    }

    @Test
    fun likeEscapeAsSecondArgument() {
        checkInputThrowingParserException("SELECT a, b FROM data WHERE a LIKE ESCAPE '\\' b",
                                          ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 36L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("escape")))
    }

    @Test
    fun atOperatorOnNonIdentifier() {
        checkInputThrowingParserException("@(a)",
                                          ErrorCode.PARSE_MISSING_IDENT_AFTER_AT,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 1L,
                                                Property.TOKEN_TYPE to TokenType.OPERATOR,
                                                Property.TOKEN_VALUE to ion.newSymbol("@")))
    }

    @Test
    fun atOperatorDoubleOnIdentifier() {
        checkInputThrowingParserException("@ @a",
                                          ErrorCode.PARSE_MISSING_IDENT_AFTER_AT,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 1L,
                                                Property.TOKEN_TYPE to TokenType.OPERATOR,
                                                Property.TOKEN_VALUE to ion.newSymbol("@")))
    }

    @Test
    fun nullIsNullIonLiteral() {
        checkInputThrowingParserException("NULL is `null`",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 9L,
                                                Property.TOKEN_TYPE to TokenType.ION_LITERAL,
                                                Property.TOKEN_VALUE to ion.newNull()))
    }

    @Test
    fun idIsStringLiteral() {
        checkInputThrowingParserException("a is 'missing'",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 6L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newString("missing")))
    }

    @Test
    fun idIsGroupMissing() {
        checkInputThrowingParserException("a is (missing)",
                                          ErrorCode.PARSE_EXPECTED_TYPE_NAME ,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 6L,
                                                Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol("(")))
    }

    @Test
    fun selectWithFromAtAndAs() {
        checkInputThrowingParserException("SELECT ord, val FROM table1 AT ord AS val",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN ,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 36L,
                                                Property.TOKEN_TYPE to TokenType.AS,
                                                Property.TOKEN_VALUE to ion.newSymbol("as")))
    }

    // FIXME This is still an error--but an error in a different way
    @Ignore
    @Test
    fun selectNothing() {
        checkInputThrowingParserException("SELECT FROM table1",
                                          ErrorCode.PARSE_UNEXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 8L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun pivotNoAt() {
        checkInputThrowingParserException("PIVOT v FROM data",
                                          ErrorCode.PARSE_EXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 9L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.KEYWORD to "AT",
                                                Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun callExtractMissingFrom() {
        checkInputThrowingParserException("extract(year b)",
                                          ErrorCode.PARSE_EXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 14L,
                                                Property.KEYWORD to "FROM",
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.TOKEN_VALUE to ion.newSymbol("b")))
    }

    @Test
    fun callExtractMissingFromWithComma() {
        checkInputThrowingParserException("extract(year, b)",
                                          ErrorCode.PARSE_EXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 13L,
                                                Property.KEYWORD to "FROM",
                                                Property.TOKEN_TYPE to TokenType.COMMA,
                                                Property.TOKEN_VALUE to ion.newSymbol(",")))
    }

    @Test
    fun callExtractMissingSecondArgument() {
        checkInputThrowingParserException("extract(year from)",
                                          ErrorCode.PARSE_UNEXPECTED_TERM,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 18L,
                                                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callExtractMissingDatePart() {
        checkInputThrowingParserException("extract(from b)",
                                          ErrorCode.PARSE_EXPECTED_DATE_PART,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 9L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("from")))
    }

    @Test
    fun callExtractOnlySecondArgument() {
        checkInputThrowingParserException("extract(b)",
                                          ErrorCode.PARSE_EXPECTED_DATE_PART,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 9L,
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.TOKEN_VALUE to ion.newSymbol("b")))
    }

    @Test
    fun callExtractOnlyDatePart() {
        checkInputThrowingParserException("extract(year)",
                                          ErrorCode.PARSE_EXPECTED_KEYWORD,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 13L,
                                                Property.KEYWORD to "FROM",
                                                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                                Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    // NOTE that we do not test DATE_DIFF below because the parser uses the same code for both date_add and date_diff

    @Test
    fun callDateAddNoArguments() {
        checkInputThrowingParserException("date_add()",
            ErrorCode.PARSE_EXPECTED_DATE_PART,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callDateAddInvalidDatePart() {
        checkInputThrowingParserException("date_add(foobar",
            ErrorCode.PARSE_EXPECTED_DATE_PART,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("foobar")))
    }

    @Test
    fun callDateAddOneArgument() {
        checkInputThrowingParserException("date_add(year)",
            ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 14L,
                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol(")"),
                Property.EXPECTED_TOKEN_TYPE to TokenType.COMMA))
    }

    @Test
    fun callDateAddOneArgumentTrailingComma() {
        checkInputThrowingParserException("date_add(year,)",
            ErrorCode.PARSE_UNEXPECTED_TERM,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol(")")))
    }

    @Test
    fun callDateAddTwoArguments() {
        checkInputThrowingParserException("date_add(year, b)",
            ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 17L,
                Property.TOKEN_TYPE to TokenType.RIGHT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol(")"),
                Property.EXPECTED_TOKEN_TYPE to TokenType.COMMA))
    }
    @Test
    fun callDateAddCommaAfterThirdArgument() {
        checkInputThrowingParserException("date_add(year, b, c,)",
            ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 20L,
                Property.TOKEN_TYPE to TokenType.COMMA,
                Property.TOKEN_VALUE to ion.newSymbol(","),
                Property.EXPECTED_TOKEN_TYPE to TokenType.RIGHT_PAREN))
    }

    @Test
    fun callDateAddMissingComma() {
        checkInputThrowingParserException("date_add(year a, b)",
            ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 15L,
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("a"),
                Property.EXPECTED_TOKEN_TYPE to TokenType.COMMA
            ))
    }

    @Test
    fun callDateAddMissingDatePart() {
        checkInputThrowingParserException("date_add(a, b, c)",
            ErrorCode.PARSE_EXPECTED_DATE_PART,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("a")))
    }

    @Test
    fun tokensAfterSemicolon() {
        checkInputThrowingParserException("1;1",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 3L,
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
                                                Property.TOKEN_VALUE to ion.newInt(1)))
    }

    @Test
    fun validQueriesSeparatedBySemicolon() {
        checkInputThrowingParserException("SELECT * FROM <<1>>;SELECT * FROM <<1>>",
                                          ErrorCode.PARSE_UNEXPECTED_TOKEN,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 21L,
                                                Property.TOKEN_TYPE to TokenType.KEYWORD,
                                                Property.TOKEN_VALUE to ion.newSymbol("select")))
    }

    @Test
    fun semicolonInsideExpression() {
        checkInputThrowingParserException("(1;)",
                                          ErrorCode.PARSE_EXPECTED_TOKEN_TYPE,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 3L,
                                                Property.EXPECTED_TOKEN_TYPE to TokenType.RIGHT_PAREN,
                                                Property.TOKEN_TYPE to TokenType.SEMICOLON,
                                                Property.TOKEN_VALUE to ion.newSymbol(";")))
    }


    @Test
    fun selectStarStar() = checkInputThrowingParserException(
        "SELECT *, * FROM <<1>>",
        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST,
        sourceLocationProperties(1, 8))


    @Test
    fun selectStarAliasDotStar() = checkInputThrowingParserException(
        "SELECT *, foo.* FROM <<{ a: 1 }>> as foo",
        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST,
        sourceLocationProperties(1, 8))


    @Test
    fun selectAliasDotStarStar() = checkInputThrowingParserException(
        "SELECT foo.*, * FROM <<{ a: 1 }>> as foo",
        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST,
        sourceLocationProperties(1, 15))


    @Test
    fun selectExpressionStar() = checkInputThrowingParserException(
        "SELECT 1, * FROM <<{ a: 1 }>>",
        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST,
        sourceLocationProperties(1, 11))


    @Test
    fun selectStarExpression() = checkInputThrowingParserException(
        "SELECT *, 1 FROM <<{ a: 1 }>>",
        ErrorCode.PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST,
        sourceLocationProperties(1, 8))

    @Test
    fun countDistinctStar() {
        checkInputThrowingParserException("COUNT(DISTINCT *)",
            ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 7L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("distinct")))
    }

    @Test
    fun countAllStar() {
        checkInputThrowingParserException("COUNT(ALL *)",
            ErrorCode.PARSE_UNSUPPORTED_CALL_WITH_STAR,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 7L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("all")))
    }

    @Test
    fun countExpressionStar() {
        checkInputThrowingParserException("COUNT(a, *)",
            ErrorCode.PARSE_UNEXPECTED_TERM,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 10L,
                Property.TOKEN_TYPE to TokenType.STAR,
                Property.TOKEN_VALUE to ion.newSymbol("*")))
    }

    @Test
    fun setWithNoAssignments() = checkInputThrowingParserException(
        "FROM x SET",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 11L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun setWithExpression() = checkInputThrowingParserException(
        "FROM x SET y, z",
        ErrorCode.PARSE_MISSING_SET_ASSIGNMENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 13L,
            Property.TOKEN_TYPE to TokenType.COMMA,
            Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun setWithWildcardPath() = checkInputThrowingParserException(
        "FROM x SET y.* = 5",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_TYPE to TokenType.STAR,
            Property.TOKEN_VALUE to ion.newSymbol("*")))

    @Test
    fun setWithExpressionPath() = checkInputThrowingParserException(
        "FROM x SET y[1+1] = 5",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newInt(1)))

    @Test
    fun fromWithDelete() = checkInputThrowingParserException(
        "FROM x DELETE FROM y",
        ErrorCode.PARSE_MISSING_OPERATION,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("delete")))

    @Test
    fun fromWithUpdate() = checkInputThrowingParserException(
        "FROM x UPDATE y SET a = b",
        ErrorCode.PARSE_MISSING_OPERATION,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("update")))

    @Test
    fun deleteNoFrom() = checkInputThrowingParserException(
        "DELETE x",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_TYPE to TokenType.IDENTIFIER,
            Property.TOKEN_VALUE to ion.newSymbol("x")))

    @Test
    fun deleteFromList() = checkInputThrowingParserException(
        "DELETE FROM x, y",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_TYPE to TokenType.COMMA,
            Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun deleteFromListWithAListMemberThatHasPath() = checkInputThrowingParserException(
            "DELETE FROM x.n, a",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 16L,
                    Property.TOKEN_TYPE to TokenType.COMMA,
                    Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun deleteFromListWithAListMemberThatHasAnAlias() = checkInputThrowingParserException(
            "DELETE FROM x.n.m AS y, a",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 23L,
                    Property.TOKEN_TYPE to TokenType.COMMA,
                    Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun deleteFromListWithAListMemberThatHasAnAliasAndPosition() = checkInputThrowingParserException(
            "DELETE FROM x.n.m AS y AT z, a",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 28L,
                    Property.TOKEN_TYPE to TokenType.COMMA,
                    Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun updateNoSet() = checkInputThrowingParserException(
        "UPDATE x",
        ErrorCode.PARSE_MISSING_OPERATION,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 9L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun updateWithNestedSet() = checkInputThrowingParserException(
        "UPDATE test SET x = SET test.y = 6",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("set")))

    @Test
    fun updateWithRemove() = checkInputThrowingParserException(
        "UPDATE test SET x = REMOVE y",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("remove")))

    @Test
    fun updateWithInsert() = checkInputThrowingParserException(
        "UPDATE test SET x = INSERT INTO foo VALUE 1",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("insert_into")))

    @Test
    fun updateWithDelete() = checkInputThrowingParserException(
        "UPDATE test SET x = DELETE FROM y",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("delete")))

    @Test
    fun updateWithExec() = checkInputThrowingParserException(
        "UPDATE test SET x = EXEC foo arg1, arg2",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 26L,
            Property.TOKEN_TYPE to TokenType.IDENTIFIER,
            Property.TOKEN_VALUE to ion.newSymbol("foo")))

    @Test
    fun updateWithCreateTable() = checkInputThrowingParserException(
        "UPDATE test SET x = CREATE TABLE foo",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("create")))

    @Test
    fun updateWithDropTable() = checkInputThrowingParserException(
        "UPDATE test SET x = DROP TABLE foo",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("drop")))

    @Test
    fun updateWithCreateIndex() = checkInputThrowingParserException(
        "UPDATE test SET x = CREATE INDEX ON foo (x, y.z)",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("create")))

    @Test
    fun nestedRemove() = checkInputThrowingParserException(
        "REMOVE REMOVE y",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("remove")))

    @Test
    fun nestedInsertInto() = checkInputThrowingParserException(
        "INSERT INTO foo VALUE INSERT INTO foo VALUE 1 AT bar",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 23L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("insert_into")))

    @Test
    fun selectAndRemove() = checkInputThrowingParserException(
        "SELECT REMOVE foo FROM bar",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("remove")))

    @Test
    fun selectAndRemove2() = checkInputThrowingParserException(
        "SELECT * FROM REMOVE foo",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 15L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("remove")))

    @Test
    fun updateWithDropIndex() = checkInputThrowingParserException(
        "UPDATE test SET x = DROP INDEX bar ON foo",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("drop")))

    @Test
    fun updateFromList() = checkInputThrowingParserException(
        "UPDATE x, y SET a = b",
        ErrorCode.PARSE_MISSING_OPERATION,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 9L,
            Property.TOKEN_TYPE to TokenType.COMMA,
            Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun insertValueMissingReturning() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 MODIFIED OLD foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 25L,
                Property.TOKEN_TYPE to TokenType.KEYWORD,
                Property.TOKEN_VALUE to ion.newSymbol("modified_old")))

    @Test
    fun insertValueReturningMissingReturningElem() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING",
            ErrorCode.PARSE_EXPECTED_RETURNING_CLAUSE,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 34L,
                    Property.TOKEN_TYPE to TokenType.EOF,
                    Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun insertValueReturningMissingReturningMapping() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING *",
            ErrorCode.PARSE_EXPECTED_RETURNING_CLAUSE,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 35L,
                    Property.TOKEN_TYPE to TokenType.STAR,
                    Property.TOKEN_VALUE to ion.newSymbol("*")))

    @Test
    fun insertValueReturningMissingReturningColumn() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD",
            ErrorCode.PARSE_UNEXPECTED_TERM,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 47L,
                    Property.TOKEN_TYPE to TokenType.EOF,
                    Property.TOKEN_VALUE to ion.newSymbol("EOF")))


    @Test
    fun insertValueMultiReturningMissingReturningColumn() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD , ALL OLD *",
            ErrorCode.PARSE_UNEXPECTED_TERM,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 48L,
                    Property.TOKEN_TYPE to TokenType.COMMA,
                    Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun insertValueMisSpellReturning() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURING MODIFIED OLD foo",
            ErrorCode.PARSE_UNEXPECTED_TOKEN,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 25L,
                    Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                    Property.TOKEN_VALUE to ion.newSymbol("RETURING")))

    @Test
    fun insertValueReturningInvalidReturningMapping() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING UPDATED OLD foo",
            ErrorCode.PARSE_EXPECTED_RETURNING_CLAUSE,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 35L,
                    Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                    Property.TOKEN_VALUE to ion.newSymbol("UPDATED")))

    @Test
    fun insertValueReturningInvalidReturningColumn() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD ;",
            ErrorCode.PARSE_UNEXPECTED_TERM,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 48L,
                    Property.TOKEN_TYPE to TokenType.SEMICOLON,
                    Property.TOKEN_VALUE to ion.newSymbol(";")))

    @Test
    fun insertValueReturningMultipleReturningColumn() = checkInputThrowingParserException(
            "INSERT INTO foo VALUE 1 RETURNING MODIFIED OLD a,b",
            ErrorCode.PARSE_EXPECTED_RETURNING_CLAUSE,
            mapOf(Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 50L,
                    Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                    Property.TOKEN_VALUE to ion.newSymbol("b")))

    @Test
    fun createTableWithKeyword() = checkInputThrowingParserException(
        "CREATE TABLE SELECT",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("select")))

    @Test
    fun createForUnsupportedObject() = checkInputThrowingParserException(
        "CREATE VIEW FOO",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 8L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("view")))

    @Test
    fun createTableWithNoIdentifier() = checkInputThrowingParserException(
        "CREATE TABLE",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 13L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun createTableWithOperatorAfterIdentifier() = checkInputThrowingParserException(
        "CREATE TABLE foo-bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 17L,
            Property.TOKEN_TYPE to TokenType.OPERATOR,
            Property.TOKEN_VALUE to ion.newSymbol("-")))

    @Test
    fun nestedCreateTable() = checkInputThrowingParserException(
        "CREATE TABLE CREATE TABLE foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("create")))

    @Test
    fun dropTableWithOperatorAfterIdentifier() = checkInputThrowingParserException(
        "DROP TABLE foo+bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 15L,
            Property.TOKEN_TYPE to TokenType.OPERATOR,
            Property.TOKEN_VALUE to ion.newSymbol("+")))

    @Test
    fun createIndexWithoutAnythingElse() = checkInputThrowingParserException(
        "CREATE INDEX",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 13L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun createIndexWithName() = checkInputThrowingParserException(
        "CREATE INDEX foo_index ON foo (bar)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 14L,
            Property.TOKEN_TYPE to TokenType.IDENTIFIER,
            Property.TOKEN_VALUE to ion.newSymbol("foo_index")))

    @Test
    fun createIndexNoNameNoTarget() = checkInputThrowingParserException(
        "CREATE INDEX ON (bar)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 17L,
            Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
            Property.TOKEN_VALUE to ion.newSymbol("(")))

    @Test
    fun createIndexNoNameNoKeyParenthesis() = checkInputThrowingParserException(
        "CREATE INDEX ON foo bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.IDENTIFIER,
            Property.TOKEN_VALUE to ion.newSymbol("bar")))

    @Test
    fun createIndexNoNameKeyExpression() = checkInputThrowingParserException(
        "CREATE INDEX ON foo (1+1)",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 22L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newInt(1)))

    @Test
    fun createIndexWithOperatorAtTail() = checkInputThrowingParserException(
        "CREATE INDEX ON foo (bar) + 1",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 27L,
            Property.TOKEN_TYPE to TokenType.OPERATOR,
            Property.TOKEN_VALUE to ion.newSymbol("+")))

    @Test
    fun createIndexNoNameKeyWildcardPath() = checkInputThrowingParserException(
        "CREATE INDEX ON foo (a.*)",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 24L,
            Property.TOKEN_TYPE to TokenType.STAR,
            Property.TOKEN_VALUE to ion.newSymbol("*")))

    @Test
    fun createIndexNoNameKeyExpressionPath() = checkInputThrowingParserException(
        "CREATE INDEX ON foo (a[1+1])",
        ErrorCode.PARSE_INVALID_PATH_COMPONENT,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 24L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newInt(1)))

    @Test
    fun dropIndexWithoutAnythingElse() = checkInputThrowingParserException(
        "DROP INDEX",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 11L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun dropIndexNoIdentifierNoTarget() = checkInputThrowingParserException(
        "DROP INDEX ON",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 12L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("on")))

    @Test
    fun dropIndexMissingOnKeyWord() = checkInputThrowingParserException(
        "DROP INDEX bar foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 16L,
            Property.TOKEN_TYPE to TokenType.IDENTIFIER,
            Property.TOKEN_VALUE to ion.newSymbol("foo")))

    @Test
    fun dropIndexWithExpression() = checkInputThrowingParserException(
        "DROP INDEX (1+1) on foo",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 12L,
            Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
            Property.TOKEN_VALUE to ion.newSymbol("(")))

    @Test
    fun dropIndexWithParenthesisAtTail() = checkInputThrowingParserException(
        "DROP INDEX goo ON foo (bar)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 23L,
            Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
            Property.TOKEN_VALUE to ion.newSymbol("(")))

    @Test
    fun dropIndexWithOperatorAtTail() = checkInputThrowingParserException(
        "DROP INDEX bar ON foo + 1",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 23L,
            Property.TOKEN_TYPE to TokenType.OPERATOR,
            Property.TOKEN_VALUE to ion.newSymbol("+")))

    @Test
    fun insertValueWithCollection() = checkInputThrowingParserException(
        "INSERT INTO foo VALUE spam, eggs",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 27L,
            Property.TOKEN_TYPE to TokenType.COMMA,
            Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun insertValuesWithAt() = checkInputThrowingParserException(
        "INSERT INTO foo VALUES (1, 2) AT bar",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 31L,
            Property.TOKEN_TYPE to TokenType.AT,
            Property.TOKEN_VALUE to ion.newSymbol("at")))

    @Test
    fun valueAsTopLevelExpression() = checkInputThrowingParserException(
        "VALUE 1",
        ErrorCode.PARSE_UNEXPECTED_KEYWORD,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 1L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("value")))

    @Test
    fun innerCrossJoinWithOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo INNER CROSS JOIN bar ON true",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 40L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("on")))

    @Test
    fun leftCrossJoinWithOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo LEFT CROSS JOIN bar ON true",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 39L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("on")))

    @Test
    fun rightCrossJoinWithOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo RIGHT CROSS JOIN bar ON true",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 40L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("on")))

    @Test
    fun innerJoinWithOutOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo INNER JOIN bar",
        ErrorCode.PARSE_MALFORMED_JOIN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 33L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun leftJoinWithOutOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo LEFT JOIN bar",
        ErrorCode.PARSE_MALFORMED_JOIN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 32L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun rightJoinWithOutOnCondition() = checkInputThrowingParserException(
        "SELECT * FROM foo RIGHT JOIN bar",
        ErrorCode.PARSE_MALFORMED_JOIN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 33L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun parenJoinWithoutOnClause() = checkInputThrowingParserException(
        "SELECT * FROM foo INNER JOIN (bar INNER JOIN baz ON true)",
        ErrorCode.PARSE_MALFORMED_JOIN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 58L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    //****************************************
    // EXEC clause parsing errors
    //****************************************

    @Test
    fun execNoStoredProcedureProvided() = checkInputThrowingParserException(
        "EXEC",
        ErrorCode.PARSE_NO_STORED_PROCEDURE_PROVIDED,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 5L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun execCommaBetweenStoredProcedureAndArg() = checkInputThrowingParserException(
        "EXEC foo, arg0, arg1",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 9L,
            Property.TOKEN_TYPE to TokenType.COMMA,
            Property.TOKEN_VALUE to ion.newSymbol(",")))

    @Test
    fun execArgTrailingComma() = checkInputThrowingParserException(
        "EXEC foo arg0, arg1,",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun execUnexpectedParen() = checkInputThrowingParserException(
        "EXEC foo()",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 9L,
            Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
            Property.TOKEN_VALUE to ion.newSymbol("(")))

    @Test
    fun execUnexpectedParenWithArgs() = checkInputThrowingParserException(
        "EXEC foo(arg0, arg1)",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 9L,
            Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
            Property.TOKEN_VALUE to ion.newSymbol("(")))

    @Test
    fun execAtUnexpectedLocation() = checkInputThrowingParserException(
        "EXEC EXEC",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("exec")))

    @Test
    fun execAtUnexpectedLocationAfterExec() = checkInputThrowingParserException(
        "EXEC foo EXEC",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 10L,
            Property.TOKEN_TYPE to TokenType.KEYWORD,
            Property.TOKEN_VALUE to ion.newSymbol("exec")))

    // TODO: The token in the error message here should be "exec" instead of "undrop".
    //  Check this issue for more details. https://github.com/partiql/partiql-lang-kotlin/issues/372
    @Test
    fun execAtUnexpectedLocationInExpression() = checkInputThrowingParserException(
        "SELECT * FROM (EXEC undrop 'foo')",
        ErrorCode.PARSE_UNEXPECTED_TERM,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 21L,
            Property.TOKEN_TYPE to TokenType.IDENTIFIER,
            Property.TOKEN_VALUE to ion.newSymbol("undrop")))

    @Test
    fun missingDateString() = checkInputThrowingParserException(
        "DATE",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 5L,
            Property.TOKEN_TYPE to TokenType.EOF,
            Property.TOKEN_VALUE to ion.newSymbol("EOF")))

    @Test
    fun invalidTypeIntForDateString() = checkInputThrowingParserException(
        "DATE 2012",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newInt(2012)))

    @Test
    fun invalidTypeIntForDateString2() = checkInputThrowingParserException(
        "DATE 2012-08-28",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newInt(2012)))

    @Test
    fun invalidTypeTimestampForDateString() = checkInputThrowingParserException(
        "DATE `2012-08-28`",
        ErrorCode.PARSE_UNEXPECTED_TOKEN,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.ION_LITERAL,
            Property.TOKEN_VALUE to ion.newTimestamp(Timestamp.forDay(2012, 8, 28))))

    @Test
    fun invalidDateStringFormat() = checkInputThrowingParserException(
        "DATE 'date_string'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("date_string")))

    @Test
    fun invalidDateStringFormatMissingDashes() = checkInputThrowingParserException(
        "DATE '20210310'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("20210310")))

    @Test
    fun invalidDateStringFormatUnexpectedColons() = checkInputThrowingParserException(
        "DATE '2021:03:10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("2021:03:10")))

    @Test
    fun invalidDateStringFormatInvalidDate() = checkInputThrowingParserException(
        "DATE '2021-02-29'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("2021-02-29")))

    @Test
    fun invalidDateStringFormatMMDDYYYY() = checkInputThrowingParserException(
        "DATE '03-10-2021'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("03-10-2021")))

    @Test
    fun invalidDateStringFormatDDMMYYYY() = checkInputThrowingParserException(
        "DATE '10-03-2021'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("10-03-2021")))

    @Test
    fun invalidExtendedDateString() = checkInputThrowingParserException(
        "DATE '+99999-03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("+99999-03-10")))

    @Test
    fun invalidDateStringNegativeYear() = checkInputThrowingParserException(
        "DATE '-9999-03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("-9999-03-10")))

    @Test
    fun invalidDateStringPositiveYear() = checkInputThrowingParserException(
        "DATE '+9999-03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("+9999-03-10")))

    @Test
    fun invalidDateStringNegativeMonth() = checkInputThrowingParserException(
        "DATE '2021--03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("2021--03-10")))

    @Test
    fun invalidDateStringPositiveMonth() = checkInputThrowingParserException(
        "DATE '2021-+03-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("2021-+03-10")))

    @Test
    fun invalidDateStringNegativeDay() = checkInputThrowingParserException(
        "DATE '2021-03--10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("2021-03--10")))

    @Test
    fun invalidDateStringPositiveDay() = checkInputThrowingParserException(
        "DATE '2021-03-+10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("2021-03-+10")))

    @Test
    fun invalidDateStringMonthOutOfRange() = checkInputThrowingParserException(
        "DATE '9999-300000000-10'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("9999-300000000-10")))

    @Test
    fun invalidDateStringDayOutOfRangeForOct() = checkInputThrowingParserException(
        "DATE '1999-10-32'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("1999-10-32")))

    @Test
    fun invalidDateStringDayOutOfRangeForNov() = checkInputThrowingParserException(
        "DATE '1999-11-31'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("1999-11-31")))

    @Test
    fun invalidDateStringDayPaddedZeroMissingFromMonth() = checkInputThrowingParserException(
        "DATE '1999-1-31'",
        ErrorCode.PARSE_INVALID_DATE_STRING,
        mapOf(
            Property.LINE_NUMBER to 1L,
            Property.COLUMN_NUMBER to 6L,
            Property.TOKEN_TYPE to TokenType.LITERAL,
            Property.TOKEN_VALUE to ion.newString("1999-1-31")))

}

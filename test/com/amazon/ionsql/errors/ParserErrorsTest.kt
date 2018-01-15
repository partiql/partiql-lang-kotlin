package com.amazon.ionsql.errors
import com.amazon.ionsql.*
import com.amazon.ionsql.syntax.IonSqlParser
import com.amazon.ionsql.syntax.ParserException
import com.amazon.ionsql.syntax.TokenType
import com.amazon.ionsql.util.*
import org.junit.*

class ParserErrorsTest : Base() {

    private val parser = IonSqlParser(ion)

    private fun checkInputThrowingParserException(input: String,
                                                  errorCode: ErrorCode,
                                                  expectErrorContextValues: Map<Property, Any>) {

        softAssert {
            try {
                parser.parse(input)
                fail("Expected ParserException but there was no Exception")
            }
            catch (pex: ParserException) {
                checkErrorAndErrorContext(errorCode, pex, expectErrorContextValues)
            }
            catch (ex: Exception) {
                fail("Expected ParserException but a different exception was thrown \n\t  $ex")
            }
        }
    }

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
                Property.TOKEN_TYPE to TokenType.LITERAL,
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

    @Test // https://i.amazon.com/issues/IONSQL-166
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
            ErrorCode.PARSE_EXPECTED_IDENT_FOR_AT,
            mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 23L,
                Property.TOKEN_TYPE to TokenType.LITERAL,
                Property.TOKEN_VALUE to ion.newBool(true)))

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
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
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
                                                Property.TOKEN_TYPE to TokenType.LITERAL,
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
    fun callExtractInvalidDatePart() {
        checkInputThrowingParserException("extract(foobar from b)",
                                          ErrorCode.PARSE_EXPECTED_DATE_PART,
                                          mapOf(Property.LINE_NUMBER to 1L,
                                                Property.COLUMN_NUMBER to 9L,
                                                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                                                Property.TOKEN_VALUE to ion.newSymbol("foobar")))
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
}
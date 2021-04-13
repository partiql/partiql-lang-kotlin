package org.partiql.lang.syntax

import com.amazon.ion.IonValue
import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.softAssert
import org.partiql.lang.util.to
import java.time.Instant
import java.time.ZoneOffset

class SqlParserDateTimeTests : SqlParserTestBase() {

    private val LOCAL_TIME_ZONE_OFFSET = (ZoneOffset.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 60).toLong()

    data class DateTimeTestCase(val source: String, val block: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode)

    @Test
    @Parameters
    fun dateLiteralTests(tc: DateTimeTestCase) = assertExpression(tc.source, tc.block)

    fun parametersForDateLiteralTests() = listOf(
        DateTimeTestCase("DATE '2012-02-29'") {
            date(2012, 2, 29)
        },
        DateTimeTestCase("DATE'1992-11-30'") {
            date(1992, 11, 30)
        },
        DateTimeTestCase("SELECT DATE '2021-03-10' FROM foo") {
            select(
                project = projectList(projectExpr(date(2021, 3, 10))),
                from = scan(id("foo"))
            )
        },
        DateTimeTestCase("TIME '02:30:59'") {
            litTime(timeValue(2, 30, 59, 0, 9, null))
        },
        DateTimeTestCase("TIME (3) '12:59:31'") {
            litTime(timeValue(12, 59, 31, 0, 3, null))
        },
        DateTimeTestCase("TIME '23:59:59.9999'") {
            litTime(timeValue(23, 59, 59, 999900000, 9, null))
        },
        DateTimeTestCase("TIME (7) '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 7, null))
        },
        DateTimeTestCase("TIME (9) '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, null))
        },
        DateTimeTestCase("TIME (0) '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 0, null))
        },
        DateTimeTestCase("TIME (10) '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, null))
        },
        DateTimeTestCase("TIME '02:30:59-05:30'") {
            litTime(timeValue(2, 30, 59, 0, 9, null))
        },
        DateTimeTestCase("TIME '02:30:59+05:30'") {
            litTime(timeValue(2, 30, 59, 0, 9, null))
        },
        DateTimeTestCase("TIME '02:30:59-14:39'") {
            litTime(timeValue(2, 30, 59, 0, 9, null))
        },
        DateTimeTestCase("TIME '02:30:59+00:00'") {
            litTime(timeValue(2, 30, 59, 0, 9, null))
        },
        DateTimeTestCase("TIME '02:30:59-00:00'") {
            litTime(timeValue(2, 30, 59, 0, 9, null))
        },
        DateTimeTestCase("TIME (3) '12:59:31+10:30'") {
            litTime(timeValue(12, 59, 31, 0, 3, null))
        },
        DateTimeTestCase("TIME (0) '00:00:00+00:00'") {
            litTime(timeValue(0, 0, 0, 0, 0, null))
        },
        DateTimeTestCase("TIME (0) '00:00:00-00:00'") {
            litTime(timeValue(0, 0, 0, 0, 0, null))
        },
        DateTimeTestCase("TIME '23:59:59.9999-11:59'") {
            litTime(timeValue(23, 59, 59, 999900000, 9, null))
        },
        DateTimeTestCase("TIME (7) '23:59:59.123456789+01:00'") {
            litTime(timeValue(23, 59, 59, 123456789, 7, null))
        },
        DateTimeTestCase("TIME (9) '23:59:59.123456789-14:50'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, null))
        },
        DateTimeTestCase("TIME (0) '23:59:59.123456789-18:00'") {
            litTime(timeValue(23, 59, 59, 123456789, 0, null))
        },
        DateTimeTestCase("TIME (10) '23:59:59.123456789+18:00'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, null))
        },
        DateTimeTestCase("TIME WITH TIME ZONE '02:30:59'") {
            litTime(timeValue(2, 30, 59, 0, 9, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME (3) WITH TIME ZONE '12:59:31'") {
            litTime(timeValue(12, 59, 31, 0, 3, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.9999'") {
            litTime(timeValue(23, 59, 59, 999900000, 9, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME (7) WITH TIME ZONE '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 7, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME (9) WITH TIME ZONE '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME (0) WITH TIME ZONE '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 0, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME (10) WITH TIME ZONE '23:59:59.123456789'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, LOCAL_TIME_ZONE_OFFSET))
        },
        DateTimeTestCase("TIME (0) WITH TIME ZONE '00:00:00+00:00'") {
            litTime(timeValue(0, 0, 0, 0, 0, 0))
        },
        DateTimeTestCase("TIME (0) WITH TIME ZONE '00:00:00-00:00'") {
            litTime(timeValue(0, 0, 0, 0, 0, 0))
        },
        DateTimeTestCase("TIME WITH TIME ZONE '02:30:59-05:30'") {
            litTime(timeValue(2, 30, 59, 0, 9, -330))
        },
        DateTimeTestCase("TIME WITH TIME ZONE '02:30:59+05:30'") {
            litTime(timeValue(2, 30, 59, 0, 9, 330))
        },
        DateTimeTestCase("TIME WITH TIME ZONE '02:30:59-14:39'") {
            litTime(timeValue(2, 30, 59, 0, 9, -879))
        },
        DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.9999-11:59'") {
            litTime(timeValue(23, 59, 59, 999900000, 9, -719))
        },
        DateTimeTestCase("TIME (7) WITH TIME ZONE '23:59:59.123456789+01:00'") {
            litTime(timeValue(23, 59, 59, 123456789, 7, 60))
        },
        DateTimeTestCase("TIME (9) WITH TIME ZONE '23:59:59.123456789-14:50'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, -890))
        },
        DateTimeTestCase("TIME (0) WITH TIME ZONE '23:59:59.123456789-18:00'") {
            litTime(timeValue(23, 59, 59, 123456789, 0, -1080))
        },
        DateTimeTestCase("TIME (10) WITH TIME ZONE '23:59:59.123456789+18:00'") {
            litTime(timeValue(23, 59, 59, 123456789, 9, 1080))
        }
    )

    private fun createErrorCaseForTime(source: String, errorCode: ErrorCode, line: Long, col: Long, tokenType: TokenType, tokenValue: IonValue): () -> Unit = {
        checkInputThrowingParserException(
            source,
            errorCode,
            mapOf(
                Property.LINE_NUMBER to line,
                Property.COLUMN_NUMBER to col,
                Property.TOKEN_TYPE to tokenType,
                Property.TOKEN_VALUE to tokenValue))
    }

    private fun createErrorCaseForTime(source: String, errorCode: ErrorCode, errorContext: Map<Property, Any>): () -> Unit = {
        checkInputThrowingParserException(
            source,
            errorCode,
            errorContext)
    }

    fun parametersForTimeParserErrorTests() = listOf(
        createErrorCaseForTime(
            source = "TIME",
            line = 1L,
            col = 5L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.EOF,
            tokenValue = ion.newSymbol("EOF")
        ),
        createErrorCaseForTime(
            source = "TIME 123",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newInt(123)
        ),
        createErrorCaseForTime(
            source = "TIME 'time_string'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("time_string")
        ),
        createErrorCaseForTime(
            source = "TIME 123.23",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.singleValue("123.23")
        ),
        createErrorCaseForTime(
            source = "TIME `2012-12-12`",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.ION_LITERAL,
            tokenValue = ion.singleValue("2012-12-12")
        ),
        createErrorCaseForTime(
            source = "TIME '2012-12-12'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("2012-12-12")
        ),
        createErrorCaseForTime(
            source = "TIME '12'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("12")
        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '12:30'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("12:30")
        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '34:59'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("34:59")
        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '59.12345'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("59.12345")
        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '1:30:38'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("1:30:38")
        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '1:30:38'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("1:30:38")
        ),
        createErrorCaseForTime(
            source = "TIME '12:59:61.0000'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("12:59:61.0000")
        ),
        createErrorCaseForTime(
            source = "TIME '12.123:45.123:54.123'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("12.123:45.123:54.123")
        ),
        createErrorCaseForTime(
            source = "TIME '-19:45:13'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("-19:45:13")
        ),
        createErrorCaseForTime(
            source = "TIME '24:00:00'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("24:00:00")
        ),
        createErrorCaseForTime(
            source = "TIME '23:59:59.99999 05:30'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59.99999 05:30")
        ),
        createErrorCaseForTime(
            source = "TIME '23:59:59+05:30.00'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59+05:30.00")
        ),
        // TODO: Investing why the build failed in GH actions for these two tests.
//        createErrorCaseForTime(
//            source = "TIME '23:59:59+24:00'",
//            line = 1L,
//            col = 6L,
//            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
//            tokenType = TokenType.LITERAL,
//            tokenValue = ion.newString("23:59:59+24:00")
//        ),
//        createErrorCaseForTime(
//            source = "TIME '23:59:59-24:00'",
//            line = 1L,
//            col = 6L,
//            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
//            tokenType = TokenType.LITERAL,
//            tokenValue = ion.newString("23:59:59-24:00")
//        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '08:59:59.99999 AM'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("08:59:59.99999 AM")
        ),
        // This is a valid time string in PostgreSQL
        createErrorCaseForTime(
            source = "TIME '08:59:59.99999 PM'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("08:59:59.99999 PM")
        ),
        createErrorCaseForTime(
            source = "TIME ( '23:59:59.99999'",
            line = 1L,
            col = 8L,
            errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59.99999")
        ),
        createErrorCaseForTime(
            source = "TIME () '23:59:59.99999'",
            line = 1L,
            col = 7L,
            errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
            tokenType = TokenType.RIGHT_PAREN,
            tokenValue = ion.newSymbol(")")
        ),
        createErrorCaseForTime(
            source = "TIME [4] '23:59:59.99999'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.LEFT_BRACKET,
            tokenValue = ion.newSymbol("[")
        ),
        createErrorCaseForTime(
            source = "TIME {4} '23:59:59.99999'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.LEFT_CURLY,
            tokenValue = ion.newSymbol("{")
        ),
        createErrorCaseForTime(
            source = "TIME 4 '23:59:59.99999'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newInt(4)
        ),
        createErrorCaseForTime(
            source = "TIME ('4') '23:59:59.99999'",
            line = 1L,
            col = 7L,
            errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("4")
        ),
        createErrorCaseForTime(
            source = "TIME ('four') '23:59:59.99999'",
            line = 1L,
            col = 7L,
            errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("four")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE",
            line = 1L,
            col = 20L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.EOF,
            tokenValue = ion.newSymbol("EOF")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '12:20'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("12:20")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '34:59'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("34:59")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '59.12345'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("59.12345")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '12:20'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("12:20")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIMEZONE '23:59:59.99999'",
            errorCode = ErrorCode.PARSE_EXPECTED_KEYWORD,
            errorContext = mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.KEYWORD to "TIME",
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("TIMEZONE")
            )
        ),
        createErrorCaseForTime(
            source = "TIME WITH_TIME_ZONE '23:59:59.99999'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.IDENTIFIER,
            tokenValue = ion.newSymbol("WITH_TIME_ZONE")
        ),
        createErrorCaseForTime(
            source = "TIME WITHTIMEZONE '23:59:59.99999'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.IDENTIFIER,
            tokenValue = ion.newSymbol("WITHTIMEZONE")
        ),
        // PartiQL doesn't support "WITHOUT TIME ZONE" yet. TIME '<time_string>' is in effect the same as TIME WITHOUT TIME ZONE '<time_string>'
        createErrorCaseForTime(
            source = "TIME WITHOUT TIME ZONE '23:59:59.99999'",
            line = 1L,
            col = 6L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.IDENTIFIER,
            tokenValue = ion.newSymbol("WITHOUT")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME PHONE '23:59:59.99999'",
            errorCode = ErrorCode.PARSE_EXPECTED_KEYWORD,
            errorContext = mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 16L,
                Property.KEYWORD to "ZONE",
                Property.TOKEN_TYPE to TokenType.IDENTIFIER,
                Property.TOKEN_VALUE to ion.newSymbol("PHONE")
            )
        ),
        createErrorCaseForTime(
            source = "TIME WITH (4) TIME ZONE '23:59:59.99999'",
            errorCode = ErrorCode.PARSE_EXPECTED_KEYWORD,
            errorContext = mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 11L,
                Property.KEYWORD to "TIME",
                Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol("(")
            )
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME (4) ZONE '23:59:59.99999'",
            errorCode = ErrorCode.PARSE_EXPECTED_KEYWORD,
            errorContext = mapOf(
                Property.LINE_NUMBER to 1L,
                Property.COLUMN_NUMBER to 16L,
                Property.KEYWORD to "ZONE",
                Property.TOKEN_TYPE to TokenType.LEFT_PAREN,
                Property.TOKEN_VALUE to ion.newSymbol("(")
            )
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE (4) '23:59:59.99999'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
            tokenType = TokenType.LEFT_PAREN,
            tokenValue = ion.newSymbol("(")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE 'time_string'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("time_string")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '23:59:59+18:00.00'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59+18:00.00")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '23:59:59-18:00.00'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59-18:00.00")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '23:59:59+18:01'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59+18:01")
        ),
        // time zone offset out of range
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '23:59:59-18:01'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59-18:01")
        ),
        // time zone offset out of range
        createErrorCaseForTime(
            source = "TIME ('4') WITH TIME ZONE '23:59:59-18:01'",
            line = 1L,
            col = 7L,
            errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("4")
        ),
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '23:59:59-18-01'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59-18-01")
        ),
        // This is valid in PostgreSQL.
        createErrorCaseForTime(
            source = "TIME WITH TIME ZONE '23:59:59 PST'",
            line = 1L,
            col = 21L,
            errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
            tokenType = TokenType.LITERAL,
            tokenValue = ion.newString("23:59:59 PST")
        )
    )

    @Test
    @Parameters
    fun timeParserErrorTests(block: () -> Unit) = block()

}
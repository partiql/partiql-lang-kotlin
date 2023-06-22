package org.partiql.lang.syntax

import com.amazon.ion.Decimal
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.ionDecimal
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.lang.util.to
import org.partiql.parser.antlr.PartiQLParser

class PartiQLParserDateTimeTests : PartiQLParserTestBase() {

    data class DateTimeTestCase(val source: String, val skipTest: Boolean = false, val block: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode)
    data class ErrorTimeTestCase(val source: String, val errorCode: ErrorCode, val ctx: Map<Property, Any>, val skipTest: Boolean = false)

    companion object {
        @JvmStatic
        fun parametersForDateLiteralTests() = listOf(
            DateTimeTestCase("DATE '2012-02-29'") {
                date(2012, 2, 29)
            },
            DateTimeTestCase("DATE'1992-11-30'") {
                date(1992, 11, 30)
            },
            DateTimeTestCase("DATE '9999-03-01'") {
                date(9999, 3, 1)
            },
            DateTimeTestCase("DATE '0000-01-01'") {
                date(0, 1, 1)
            },
            DateTimeTestCase("DATE '0000-02-29'") {
                date(0, 2, 29)
            },
            DateTimeTestCase("DATE '0000-02-29'") {
                date(0, 2, 29)
            },
            DateTimeTestCase("SELECT DATE '2021-03-10' FROM foo") {
                select(
                    project = projectList(projectExpr(date(2021, 3, 10))),
                    from = scan(id("foo"))
                )
            },
        )

        @JvmStatic
        fun parameterForTimeLiteralTests() = listOf(
            DateTimeTestCase("TIME '02:30:59'") {
                litTime(timeValue(2, 30, 59, 0, 0, false, null))
            },
            DateTimeTestCase("TIME (3) '12:59:31'") {
                litTime(timeValue(12, 59, 31, 0, 3, false, null))
            },
            DateTimeTestCase("TIME '23:59:59.9999'") {
                litTime(timeValue(23, 59, 59, 999900000, 4, false, null))
            },
            DateTimeTestCase("TIME (7) '23:59:59.123456789'") {
                litTime(timeValue(23, 59, 59, 123456789, 7, false, null))
            },
            DateTimeTestCase("TIME (9) '23:59:59.123456789'") {
                litTime(timeValue(23, 59, 59, 123456789, 9, false, null))
            },
            DateTimeTestCase("TIME (0) '23:59:59.123456789'") {
                litTime(timeValue(23, 59, 59, 123456789, 0, false, null))
            },
            DateTimeTestCase("TIME '02:30:59-05:30'") {
                litTime(timeValue(2, 30, 59, 0, 0, false, null))
            },
            DateTimeTestCase("TIME '02:30:59+05:30'") {
                litTime(timeValue(2, 30, 59, 0, 0, false, null))
            },
            DateTimeTestCase("TIME '02:30:59-14:39'") {
                litTime(timeValue(2, 30, 59, 0, 0, false, null))
            },
            DateTimeTestCase("TIME '02:30:59+00:00'") {
                litTime(timeValue(2, 30, 59, 0, 0, false, null))
            },
            DateTimeTestCase("TIME '02:30:59-00:00'") {
                litTime(timeValue(2, 30, 59, 0, 0, false, null))
            },
            DateTimeTestCase("TIME (3) '12:59:31+10:30'") {
                litTime(timeValue(12, 59, 31, 0, 3, false, null))
            },
            DateTimeTestCase("TIME (0) '00:00:00+00:00'") {
                litTime(timeValue(0, 0, 0, 0, 0, false, null))
            },
            DateTimeTestCase("TIME (0) '00:00:00-00:00'") {
                litTime(timeValue(0, 0, 0, 0, 0, false, null))
            },
            DateTimeTestCase("TIME '23:59:59.9999-11:59'") {
                litTime(timeValue(23, 59, 59, 999900000, 4, false, null))
            },
            DateTimeTestCase("TIME '23:59:59.99990-11:59'") {
                litTime(timeValue(23, 59, 59, 999900000, 5, false, null))
            },
            DateTimeTestCase("TIME (5) '23:59:59.9999-11:59'") {
                litTime(timeValue(23, 59, 59, 999900000, 5, false, null))
            },
            DateTimeTestCase("TIME (7) '23:59:59.123456789+01:00'") {
                litTime(timeValue(23, 59, 59, 123456789, 7, false, null))
            },
            DateTimeTestCase("TIME (9) '23:59:59.123456789-14:50'") {
                litTime(timeValue(23, 59, 59, 123456789, 9, false, null))
            },
            DateTimeTestCase("TIME (0) '23:59:59.123456789-18:00'") {
                litTime(timeValue(23, 59, 59, 123456789, 0, false, null))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '02:30:59'") {
                litTime(timeValue(2, 30, 59, 0, 0, true, null))
            },
            DateTimeTestCase("TIME (3) WITH TIME ZONE '12:59:31'") {
                litTime(timeValue(12, 59, 31, 0, 3, true, null))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.9999'") {
                litTime(timeValue(23, 59, 59, 999900000, 4, true, null))
            },
            DateTimeTestCase("TIME (7) WITH TIME ZONE '23:59:59.123456789'") {
                litTime(timeValue(23, 59, 59, 123456789, 7, true, null))
            },
            DateTimeTestCase("TIME (9) WITH TIME ZONE '23:59:59.123456789'") {
                litTime(timeValue(23, 59, 59, 123456789, 9, true, null))
            },
            DateTimeTestCase("TIME (0) WITH TIME ZONE '23:59:59.123456789'") {
                litTime(timeValue(23, 59, 59, 123456789, 0, true, null))
            },
            DateTimeTestCase("TIME (0) WITH TIME ZONE '00:00:00+00:00'") {
                litTime(timeValue(0, 0, 0, 0, 0, true, 0))
            },
            DateTimeTestCase("TIME (0) WITH TIME ZONE '00:00:00.0000-00:00'") {
                litTime(timeValue(0, 0, 0, 0, 0, true, 0))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '02:30:59.1234500-05:30'") {
                litTime(timeValue(2, 30, 59, 123450000, 7, true, -330))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '02:30:59+05:30'") {
                litTime(timeValue(2, 30, 59, 0, 0, true, 330))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '02:30:59-14:39'") {
                litTime(timeValue(2, 30, 59, 0, 0, true, -879))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.9999-11:59'") {
                litTime(timeValue(23, 59, 59, 999900000, 4, true, -719))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.99990-11:59'") {
                litTime(timeValue(23, 59, 59, 999900000, 5, true, -719))
            },
            DateTimeTestCase("TIME (5) WITH TIME ZONE '23:59:59.9999-11:59'") {
                litTime(timeValue(23, 59, 59, 999900000, 5, true, -719))
            },
            DateTimeTestCase("TIME (7) WITH TIME ZONE '23:59:59.123456789+01:00'") {
                litTime(timeValue(23, 59, 59, 123456789, 7, true, 60))
            },
            DateTimeTestCase("TIME (9) WITH TIME ZONE '23:59:59.123456789-14:50'") {
                litTime(timeValue(23, 59, 59, 123456789, 9, true, -890))
            },
            DateTimeTestCase("TIME (0) WITH TIME ZONE '23:59:59.123456789-18:00'") {
                litTime(timeValue(23, 59, 59, 123456789, 0, true, -1080))
            },
            // TODO: These tests should pass. Check https://github.com/partiql/partiql-lang-kotlin/issues/395
            DateTimeTestCase("TIME '23:59:59.1234567890'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456789, 9, false, null))
            },
            DateTimeTestCase("TIME '23:59:59.1234567899'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456790, 9, false, null))
            },
            DateTimeTestCase("TIME '23:59:59.1234567890+18:00'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456789, 9, false, null))
            },
            DateTimeTestCase("TIME '23:59:59.1234567899+18:00'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456790, 9, false, null))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.1234567890'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456789, 9, true, null))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.1234567899'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456790, 9, true, null))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.1234567890+18:00'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456789, 9, true, 1080))
            },
            DateTimeTestCase("TIME WITH TIME ZONE '23:59:59.1234567899+18:00'", skipTest = true) {
                litTime(timeValue(23, 59, 59, 123456790, 9, true, 1080))
            }
        )

        @JvmStatic
        fun parameterForTimestampLiteralTests() = listOf(
            // TIMESTAMP WITHOUT TIME ZONE
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        null, null, null,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05.678'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.678")).asAnyElement(),
                        null, null, null,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05.678901234567890'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.678901234567890")).asAnyElement(),
                        null, null, null,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP(1) '2023-01-02 03:04:05.678901234567890'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.678901234567890")).asAnyElement(),
                        null, null, null,
                        1,
                    )
                )
            },
            // TIMESTAMP WITH TIME ZONE
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05+06:07'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "+", 6, 7,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05-06:07'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "-", 6, 7,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "-", 0, 0,
                        null,
                    )
                )
            },
            // multiple space
            DateTimeTestCase("TIMESTAMP '2023-01-02    03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "-", 0, 0,
                        null,
                    )
                )
            },
            // T as delimiter
            DateTimeTestCase("TIMESTAMP '2023-01-02T03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "-", 0, 0,
                        null,
                    )
                )
            },
            // t as delimiter
            DateTimeTestCase("TIMESTAMP '2023-01-02t03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "-", 0, 0,
                        null,
                    )
                )
            },
            // Z for UTC
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05Z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "+", 0, 0,
                        null,
                    )
                )
            },
            // z for UTC
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "+", 0, 0,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02T03:04:05Z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "+", 0, 0,
                        null,
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02t03:04:05z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        "+", 0, 0,
                        null,
                    )
                )
            },

        )

        // TODO: Adding failing test cases for Parser
        @JvmStatic
        fun parametersForTimeParserErrorTests() = listOf(
            createErrorCaseForTime(
                source = "TIME",
                line = 1L,
                col = 5L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.EOF,
                tokenValue = ION.newSymbol("EOF")
            ),
            createErrorCaseForTime(
                source = "TIME 123",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(123)
            ),
            createErrorCaseForTime(
                source = "TIME 'time_string'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("time_string")
            ),
            createErrorCaseForTime(
                source = "TIME 123.23",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_DECIMAL,
                tokenValue = ION.singleValue("123.23")
            ),
            createErrorCaseForTime(
                source = "TIME `2012-12-12`",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.ION_CLOSURE,
                tokenValue = ION.singleValue("2012-12-12")
            ),
            createErrorCaseForTime(
                source = "TIME '2012-12-12'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2012-12-12")
            ),
            createErrorCaseForTime(
                source = "TIME '12'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '12:30'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:30")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '34:59'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("34:59")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '59.12345'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("59.12345")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '1:30:38'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("1:30:38")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '1:30:38'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("1:30:38")
            ),
            createErrorCaseForTime(
                source = "TIME '12:59:61.0000'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:59:61.0000")
            ),
            createErrorCaseForTime(
                source = "TIME '12.123:45.123:54.123'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12.123:45.123:54.123")
            ),
            createErrorCaseForTime(
                source = "TIME '-19:45:13'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("-19:45:13")
            ),
            createErrorCaseForTime(
                source = "TIME '24:00:00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("24:00:00")
            ),
            createErrorCaseForTime(
                source = "TIME '23:59:59.99999 05:30'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59.99999 05:30")
            ),
            createErrorCaseForTime(
                source = "TIME '23:59:59+05:30.00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+05:30.00")
            ),
            // TODO: Investigate why the build fails in GH actions for these two tests.
            createErrorCaseForTime(
                source = "TIME '23:59:59+24:00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+24:00"),
                skipTest = true
            ),
            createErrorCaseForTime(
                source = "TIME '23:59:59-24:00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-24:00"),
                skipTest = true
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '08:59:59.99999 AM'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("08:59:59.99999 AM")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForTime(
                source = "TIME '08:59:59.99999 PM'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("08:59:59.99999 PM")
            ),
            createErrorCaseForTime(
                source = "TIME ( '23:59:59.99999'",
                line = 1L,
                col = 8L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59.99999"),
            ),
            createErrorCaseForTime(
                source = "TIME () '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.PAREN_RIGHT,
                tokenValue = ION.newSymbol(")")
            ),
            createErrorCaseForTime(
                source = "TIME [4] '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.BRACKET_LEFT,
                tokenValue = ION.newSymbol("[")
            ),
            createErrorCaseForTime(
                source = "TIME {4} '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.BRACE_LEFT,
                tokenValue = ION.newSymbol("{")
            ),
            createErrorCaseForTime(
                source = "TIME 4 '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(4)
            ),
            createErrorCaseForTime(
                source = "TIME ('4') '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("4"),
            ),
            createErrorCaseForTime(
                source = "TIME (-1) '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.MINUS,
                tokenValue = ION.newSymbol("-")
            ),
            createErrorCaseForTime(
                source = "TIME (10) '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(10)
            ),
            createErrorCaseForTime(
                source = "TIME ('four') '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("four")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE",
                line = 1L,
                col = 20L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.EOF,
                tokenValue = ION.newSymbol("EOF")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '12:20'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:20")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '34:59'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("34:59")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '59.12345'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("59.12345")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '12:20'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:20")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIMEZONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                line = 1L,
                col = 11L,
                tokenType = PartiQLParser.IDENTIFIER,
                tokenValue = ION.newSymbol("TIMEZONE")
            ),
            createErrorCaseForTime(
                source = "TIME WITH_TIME_ZONE '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.IDENTIFIER,
                tokenValue = ION.newSymbol("WITH_TIME_ZONE")
            ),
            createErrorCaseForTime(
                source = "TIME WITHTIMEZONE '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.IDENTIFIER,
                tokenValue = ION.newSymbol("WITHTIMEZONE")
            ),
            // PartiQL doesn't support "WITHOUT TIME ZONE" yet. TIME '<time_string>' is in effect the same as TIME WITHOUT TIME ZONE '<time_string>'
            createErrorCaseForTime(
                source = "TIME WITHOUT TIME ZONE '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.IDENTIFIER,
                tokenValue = ION.newSymbol("WITHOUT")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME PHONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 16L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.IDENTIFIER.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("PHONE")
                )
            ),
            createErrorCaseForTime(
                source = "TIME WITH (4) TIME ZONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 11L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("(")
                )
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME (4) ZONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 16L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("(")
                )
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE (4) '23:59:59.99999'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.PAREN_LEFT,
                tokenValue = ION.newSymbol("(")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE 'time_string'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("time_string")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '23:59:59+18:00.00'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+18:00.00")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '23:59:59-18:00.00'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-18:00.00")
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '23:59:59+18:01'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+18:01")
            ),
            // time zone offset out of range
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '23:59:59-18:01'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-18:01")
            ),
            // time zone offset out of range
            createErrorCaseForTime(
                source = "TIME ('4') WITH TIME ZONE '23:59:59-18:01'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("4"),
            ),
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '23:59:59-18-01'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-18-01")
            ),
            // This is valid in PostgreSQL.
            createErrorCaseForTime(
                source = "TIME WITH TIME ZONE '23:59:59 PST'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59 PST")
            )
        )

        private fun createErrorCaseForTime(source: String, errorCode: ErrorCode, line: Long, col: Long, tokenType: Int, tokenValue: IonValue, skipTest: Boolean = false): ErrorTimeTestCase {
            val displayTokenType = tokenType.getAntlrDisplayString()
            val ctx = mapOf(
                Property.LINE_NUMBER to line,
                Property.COLUMN_NUMBER to col,
                Property.TOKEN_DESCRIPTION to displayTokenType,
                Property.TOKEN_VALUE to tokenValue
            )
            return ErrorTimeTestCase(source, errorCode, ctx, skipTest)
        }

        private fun createErrorCaseForTime(source: String, errorCode: ErrorCode, errorContext: Map<Property, Any>) = ErrorTimeTestCase(source, errorCode, errorContext)

    }


    private fun runDateTimeTest(tc: DateTimeTestCase) = if (!tc.skipTest) {
        assertExpression(tc.source, expectedPigBuilder = tc.block)
    } else {
        // Skip test, do nothing
    }

    @ParameterizedTest
    @MethodSource("parametersForDateLiteralTests")
    fun dateLiteralTests(tc: DateTimeTestCase) = runDateTimeTest(tc)

    @ParameterizedTest
    @MethodSource("parameterForTimeLiteralTests")
    fun timeLiteralTests(tc: DateTimeTestCase) = runDateTimeTest(tc)

    @ParameterizedTest
    @MethodSource("parameterForTimestampLiteralTests")
    fun timestampLiteralTests(tc: DateTimeTestCase) = runDateTimeTest(tc)



    private fun runErrorTimeTestCase(tc: ErrorTimeTestCase) {
        if (!tc.skipTest) {
            checkInputThrowingParserException(
                tc.source,
                tc.errorCode,
                tc.ctx
            )
        }
    }
    @ParameterizedTest
    @MethodSource("parametersForTimeParserErrorTests")
    fun timeParserErrorTests(tc: ErrorTimeTestCase) = runErrorTimeTestCase(tc)
}

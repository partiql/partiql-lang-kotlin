package org.partiql.lang.syntax

import com.amazon.ion.Decimal
import com.amazon.ion.IonValue
import com.amazon.ionelement.api.ionDecimal
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.vr
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.lang.util.to
import org.partiql.parser.antlr.PartiQLParser

class PartiQLParserDateTimeTests : PartiQLParserTestBase() {

    // TODO we do not model precision within the expression node
    // For example, TIME (0) WITH TIME ZONE '23:59:59.123456789'` will have precision 0 which means
    // the underlying literal value does not preserve the extraneous places.
    // We should consider preserving the AST exactly as is text.
    // override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)
    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT)

    data class DateTimeTestCase(
        val source: String,
        val skipTest: Boolean = false,
        val block: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode,
    )

    data class ErrorDateTimeTestCase(
        val source: String,
        val errorCode: ErrorCode,
        val ctx: Map<Property, Any>,
        val skipTest: Boolean = false,
    )

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
                    from = scan(vr("foo"))
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
                        null, null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05.678'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.678")).asAnyElement(),
                        null, null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05.678901234567890'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.678901234567890")).asAnyElement(),
                        null, null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP(1) '2023-01-02 03:04:05.678901234567890'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.678901234567890")).asAnyElement(),
                        null, 1
                    )
                )
            },
            // TIMESTAMP WITH TIME ZONE
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05+06:07'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(367L), null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05-06:07'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(-367L), null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        unknownTimezone(), null,
                    )
                )
            },
            // multiple space
            DateTimeTestCase("TIMESTAMP '2023-01-02    03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        unknownTimezone(), null
                    )
                )
            },
            // T as delimiter
            DateTimeTestCase("TIMESTAMP '2023-01-02T03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        unknownTimezone(), null
                    )
                )
            },
            // t as delimiter
            DateTimeTestCase("TIMESTAMP '2023-01-02t03:04:05-00:00'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        unknownTimezone(), null
                    )
                )
            },
            // Z for UTC
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05Z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(0L), null
                    )
                )
            },
            // z for UTC
            DateTimeTestCase("TIMESTAMP '2023-01-02 03:04:05z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(0L), null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02T03:04:05Z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(0L), null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP '2023-01-02t03:04:05z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(0L), null
                    )
                )
            },
            DateTimeTestCase("TIMESTAMP WITH TIME ZONE '2023-01-02T03:04:05Z'") {
                timestamp(
                    timestampValue(
                        2023, 1, 2,
                        3, 4, ionDecimal(Decimal.valueOf("5.")).asAnyElement(),
                        utcOffset(0L), null
                    )
                )
            },
        )

        @JvmStatic
        fun parametersForTimeParserErrorTests() = listOf(
            createErrorCaseForDateTime(
                source = "TIME",
                line = 1L,
                col = 5L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.EOF,
                tokenValue = ION.newSymbol("EOF")
            ),
            createErrorCaseForDateTime(
                source = "TIME 123",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(123)
            ),
            createErrorCaseForDateTime(
                source = "TIME 'time_string'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("time_string")
            ),
            createErrorCaseForDateTime(
                source = "TIME 123.23",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_DECIMAL,
                tokenValue = ION.singleValue("123.23")
            ),
            createErrorCaseForDateTime(
                source = "TIME `2012-12-12`",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.ION_CLOSURE,
                tokenValue = ION.singleValue("2012-12-12")
            ),
            createErrorCaseForDateTime(
                source = "TIME '2012-12-12'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2012-12-12")
            ),
            createErrorCaseForDateTime(
                source = "TIME '12'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '12:30'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:30")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '34:59'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("34:59")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '59.12345'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("59.12345")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '1:30:38'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("1:30:38")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '1:30:38'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("1:30:38")
            ),
            createErrorCaseForDateTime(
                source = "TIME '12:59:61.0000'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:59:61.0000")
            ),
            createErrorCaseForDateTime(
                source = "TIME '12.123:45.123:54.123'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12.123:45.123:54.123")
            ),
            createErrorCaseForDateTime(
                source = "TIME '-19:45:13'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("-19:45:13")
            ),
            createErrorCaseForDateTime(
                source = "TIME '24:00:00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("24:00:00")
            ),
            createErrorCaseForDateTime(
                source = "TIME '23:59:59.99999 05:30'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59.99999 05:30")
            ),
            createErrorCaseForDateTime(
                source = "TIME '23:59:59+05:30.00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+05:30.00")
            ),
            // TODO: Investigate why the build fails in GH actions for these two tests.
            createErrorCaseForDateTime(
                source = "TIME '23:59:59+24:00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+24:00"),
                skipTest = true
            ),
            createErrorCaseForDateTime(
                source = "TIME '23:59:59-24:00'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-24:00"),
                skipTest = true
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '08:59:59.99999 AM'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("08:59:59.99999 AM")
            ),
            // This is a valid time string in PostgreSQL
            createErrorCaseForDateTime(
                source = "TIME '08:59:59.99999 PM'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("08:59:59.99999 PM")
            ),
            createErrorCaseForDateTime(
                source = "TIME ( '23:59:59.99999'",
                line = 1L,
                col = 8L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59.99999"),
            ),
            createErrorCaseForDateTime(
                source = "TIME () '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.PAREN_RIGHT,
                tokenValue = ION.newSymbol(")")
            ),
            createErrorCaseForDateTime(
                source = "TIME [4] '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.BRACKET_LEFT,
                tokenValue = ION.newSymbol("[")
            ),
            createErrorCaseForDateTime(
                source = "TIME {4} '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.BRACE_LEFT,
                tokenValue = ION.newSymbol("{")
            ),
            createErrorCaseForDateTime(
                source = "TIME 4 '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(4)
            ),
            createErrorCaseForDateTime(
                source = "TIME ('4') '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("4"),
            ),
            createErrorCaseForDateTime(
                source = "TIME (-1) '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.MINUS,
                tokenValue = ION.newSymbol("-")
            ),
            createErrorCaseForDateTime(
                source = "TIME (10) '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(10)
            ),
            createErrorCaseForDateTime(
                source = "TIME ('four') '23:59:59.99999'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("four")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE",
                line = 1L,
                col = 20L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.EOF,
                tokenValue = ION.newSymbol("EOF")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '12:20'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:20")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '34:59'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("34:59")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '59.12345'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("59.12345")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '12:20'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:20")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIMEZONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                line = 1L,
                col = 11L,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("TIMEZONE")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH_TIME_ZONE '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("WITH_TIME_ZONE")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITHTIMEZONE '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("WITHTIMEZONE")
            ),
            // PartiQL doesn't support "WITHOUT TIME ZONE" yet. TIME '<time_string>' is in effect the same as TIME WITHOUT TIME ZONE '<time_string>'
            createErrorCaseForDateTime(
                source = "TIME WITHOUT TIME ZONE '23:59:59.99999'",
                line = 1L,
                col = 6L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("WITHOUT")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME PHONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 16L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.REGULAR_IDENTIFIER.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("PHONE")
                )
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH (4) TIME ZONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 11L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("(")
                )
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME (4) ZONE '23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 16L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("(")
                )
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE (4) '23:59:59.99999'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.PAREN_LEFT,
                tokenValue = ION.newSymbol("(")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE 'time_string'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("time_string")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '23:59:59+18:00.00'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+18:00.00")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '23:59:59-18:00.00'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-18:00.00")
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '23:59:59+18:01'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59+18:01")
            ),
            // time zone offset out of range
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '23:59:59-18:01'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-18:01")
            ),
            // time zone offset out of range
            createErrorCaseForDateTime(
                source = "TIME ('4') WITH TIME ZONE '23:59:59-18:01'",
                line = 1L,
                col = 7L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("4"),
            ),
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '23:59:59-18-01'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59-18-01")
            ),
            // This is valid in PostgreSQL.
            createErrorCaseForDateTime(
                source = "TIME WITH TIME ZONE '23:59:59 PST'",
                line = 1L,
                col = 21L,
                errorCode = ErrorCode.PARSE_INVALID_TIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("23:59:59 PST")
            )
        )

        @JvmStatic
        fun parametersForTimestampParserErrorTests() = listOf(
            createErrorCaseForDateTime(
                source = "TIMESTAMP",
                line = 1L,
                col = 10L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.EOF,
                tokenValue = ION.newSymbol("EOF")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP 123",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(123)
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP 'timestamp_string'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("timestamp_string")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP 123.23",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_DECIMAL,
                tokenValue = ION.singleValue("123.23")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP `2012-12-12`",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.ION_CLOSURE,
                tokenValue = ION.singleValue("2012-12-12")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2012-12-12'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2012-12-12")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '12'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '12:30'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("12:30")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 34:59'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 34:59")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2021-01-02 59.12345'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2021-01-02 59.12345")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-01 1:30:38'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-01 1:30:38")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 12:59:61.0000'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 12:59:61.0000")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 12.123:45.123:54.123'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 12.123:45.123:54.123")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 -19:45:13'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 -19:45:13")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 24:00:00'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 24:00:00")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 23:59:59.99999 05:30'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59.99999 05:30")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 23:59:59+05:30.00'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59+05:30.00")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 23:59:59+24:00'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59+24:00"),
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 23:59:59-24:00'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59-24:00"),
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 08:59:59.99999 AM'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 08:59:59.99999 AM")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP '2023-01-02 08:59:59.99999 PM'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 08:59:59.99999 PM")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP ( '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 13L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59.99999"),
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP () '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 12L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.PAREN_RIGHT,
                tokenValue = ION.newSymbol(")")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP [4] '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.BRACKET_LEFT,
                tokenValue = ION.newSymbol("[")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP {4} '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.BRACE_LEFT,
                tokenValue = ION.newSymbol("{")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP 4 '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_INTEGER,
                tokenValue = ION.newInt(4)
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP ('4') '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 12L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("4"),
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP (-1) '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 12L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.MINUS,
                tokenValue = ION.newSymbol("-")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP ('four') '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 12L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("four")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE",
                line = 1L,
                col = 25L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.EOF,
                tokenValue = ION.newSymbol("EOF")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 12:20'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 12:20")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 34:59'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 34:59")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 59.12345'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 59.12345")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 12:20'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 12:20")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIMEZONE '2023-01-02 23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                line = 1L,
                col = 16L,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("TIMEZONE")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH_TIME_ZONE '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("WITH_TIME_ZONE")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITHTIMEZONE '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("WITHTIMEZONE")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITHOUT TIME ZONE '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 11L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.REGULAR_IDENTIFIER,
                tokenValue = ION.newSymbol("WITHOUT")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME PHONE '2023-01-02 23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 21L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.REGULAR_IDENTIFIER.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("PHONE")
                )
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH (4) TIME ZONE '2023-01-02 23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 16L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("(")
                )
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME (4) ZONE '2023-01-02 23:59:59.99999'",
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                errorContext = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 21L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.PAREN_LEFT.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("(")
                )
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE (4) '2023-01-02 23:59:59.99999'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.PAREN_LEFT,
                tokenValue = ION.newSymbol("(")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE 'timestamp_string'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("timestamp_string")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 23:59:59+18:00.00'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59+18:00.00")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 23:59:59-18:00.00'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59-18:00.00")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 23:59:59+24:01'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59+24:01")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 23:59:59-24:01'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59-24:01")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP ('4') WITH TIME ZONE '2023-01-02 23:59:59-24:01'",
                line = 1L,
                col = 12L,
                errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("4"),
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 23:59:59 PST'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59 PST")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02 23:59:59'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02 23:59:59")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02TT23:59:59'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02TT23:59:59")
            ),
            createErrorCaseForDateTime(
                source = "TIMESTAMP WITH TIME ZONE '2023-01-02T23:59:59Zz'",
                line = 1L,
                col = 26L,
                errorCode = ErrorCode.PARSE_INVALID_DATETIME_STRING,
                tokenType = PartiQLParser.LITERAL_STRING,
                tokenValue = ION.newString("2023-01-02T23:59:59Zz")
            )
        )

        private fun createErrorCaseForDateTime(
            source: String,
            errorCode: ErrorCode,
            line: Long,
            col: Long,
            tokenType: Int,
            tokenValue: IonValue,
            skipTest: Boolean = false,
        ): ErrorDateTimeTestCase {
            val displayTokenType = tokenType.getAntlrDisplayString()
            val ctx = mapOf(
                Property.LINE_NUMBER to line,
                Property.COLUMN_NUMBER to col,
                Property.TOKEN_DESCRIPTION to displayTokenType,
                Property.TOKEN_VALUE to tokenValue
            )
            return ErrorDateTimeTestCase(source, errorCode, ctx, skipTest)
        }

        private fun createErrorCaseForDateTime(source: String, errorCode: ErrorCode, errorContext: Map<Property, Any>) =
            ErrorDateTimeTestCase(source, errorCode, errorContext)
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

    private fun runErrorTimeTestCase(tc: ErrorDateTimeTestCase) {
        if (!tc.skipTest) {
            checkInputThrowingParserException(
                tc.source,
                tc.errorCode,
                tc.ctx,
                assertContext = false,
            )
        }
    }

    @ParameterizedTest
    @MethodSource("parametersForTimeParserErrorTests")
    fun timeParserErrorTests(tc: ErrorDateTimeTestCase) = runErrorTimeTestCase(tc)

    @ParameterizedTest
    @MethodSource("parametersForTimestampParserErrorTests")
    fun timestampParserErrorTests(tc: ErrorDateTimeTestCase) = runErrorTimeTestCase(tc)
}

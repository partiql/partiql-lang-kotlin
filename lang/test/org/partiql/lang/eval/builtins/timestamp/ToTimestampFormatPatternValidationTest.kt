package org.partiql.lang.eval.builtins.timestamp

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.util.sourceLocationProperties
import org.partiql.lang.util.to

@RunWith(JUnitParamsRunner::class)
class ToTimestampFormatPatternValidationTest : EvaluatorTestBase() {

    data class ValidationTestCase(val pattern: String, val fields: String)

    @Test
    fun hourClock24HourAmPmMistmatchTest() {
        checkInputThrowingEvaluationException(
            "TO_TIMESTAMP('doesnt matter', 'yyyy M dd H m a')",
            ErrorCode.EVALUATOR_TIMESTAMP_FORMAT_PATTERN_HOUR_CLOCK_AM_PM_MISMATCH,
            sourceLocationProperties(1, 1) + mapOf(Property.TIMESTAMP_FORMAT_PATTERN to "yyyy M dd H m a"),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    @Test
    fun hourClock12HourAmPmMistmatchTest() {
        checkInputThrowingEvaluationException(
            "TO_TIMESTAMP('doesnt matter', 'yyyy M dd h m')",
            ErrorCode.EVALUATOR_TIMESTAMP_FORMAT_PATTERN_HOUR_CLOCK_AM_PM_MISMATCH,
            sourceLocationProperties(1, 1) + mapOf(Property.TIMESTAMP_FORMAT_PATTERN to "yyyy M dd h m"),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    @Test
    fun firstLetterOfMonthIsInvalidForToTimestamp() {
        checkInputThrowingEvaluationException(
            "TO_TIMESTAMP('doesnt matter', 'y MMMMM')",
            ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL_FOR_PARSING,
            sourceLocationProperties(1, 1) + mapOf(Property.TIMESTAMP_FORMAT_PATTERN to "y MMMMM"),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    fun parametersForIncompleteFormatPatternTest() = listOf(
        ValidationTestCase("", "YEAR"),
        ValidationTestCase("'some text' /-,:.", "YEAR"),
        ValidationTestCase("a", "YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY"),
        ValidationTestCase("x", "YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY"),

        ValidationTestCase("M", "YEAR"),
        ValidationTestCase("d", "YEAR, MONTH_OF_YEAR"),
        ValidationTestCase("H", "YEAR, MONTH_OF_YEAR, DAY_OF_MONTH"),
        ValidationTestCase("m", "YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY"),
        ValidationTestCase("s", "YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR"),
        ValidationTestCase("S", "YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE"),

        ValidationTestCase("yyyy-d", "MONTH_OF_YEAR"),
        ValidationTestCase("yyyy-H", "MONTH_OF_YEAR, DAY_OF_MONTH"),
        ValidationTestCase("yyyy-m", "MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY"),
        ValidationTestCase("yyyy-s", "MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR"),
        ValidationTestCase("yyyy-S", "MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE"),

        ValidationTestCase("yyyy-M-H", "DAY_OF_MONTH"),
        ValidationTestCase("yyyy-M-m", "DAY_OF_MONTH, HOUR_OF_DAY"),
        ValidationTestCase("yyyy-M-s", "DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR"),
        ValidationTestCase("yyyy-M-S", "DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE"),

        ValidationTestCase("yyyy-M-d-m", "HOUR_OF_DAY"),
        ValidationTestCase("yyyy-M-d-s", "HOUR_OF_DAY, MINUTE_OF_HOUR"),
        ValidationTestCase("yyyy-M-d-S", "HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE"),

        ValidationTestCase("yyyy-M-d-h-s", "MINUTE_OF_HOUR"),
        ValidationTestCase("yyyy-M-d-h-S", "MINUTE_OF_HOUR, SECOND_OF_MINUTE"),
        ValidationTestCase("yyyy-M-d-h-m-S", "SECOND_OF_MINUTE")
    )

    @Test
    @Parameters
    fun incompleteFormatPatternTest(testCase: ValidationTestCase) {
        checkInputThrowingEvaluationException(
            "TO_TIMESTAMP('doesnt matter', '${testCase.pattern.replace("'", "''")}')",
            ErrorCode.EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN,
            sourceLocationProperties(1, 1) + mapOf(
                Property.TIMESTAMP_FORMAT_PATTERN to testCase.pattern,
                Property.TIMESTAMP_FORMAT_PATTERN_FIELDS to testCase.fields
            ),
            expectedPermissiveModeResult = "MISSING"
        )
    }

    fun parametersForDuplicateFieldPatternTest() = listOf(

        // y, yy, and yyyy
        ValidationTestCase("y y", "YEAR"),
        ValidationTestCase("y yy", "YEAR"),
        ValidationTestCase("y yyyy", "YEAR"),
        ValidationTestCase("yy y", "YEAR"),
        ValidationTestCase("yy yy", "YEAR"),
        ValidationTestCase("yy yyyy", "YEAR"),
        ValidationTestCase("yyyy y", "YEAR"),
        ValidationTestCase("yyyy yy", "YEAR"),
        ValidationTestCase("yyyy yyyy", "YEAR"),

        // M, MM and MMMM
        ValidationTestCase("M M", "MONTH_OF_YEAR"),
        ValidationTestCase("M MM", "MONTH_OF_YEAR"),
        ValidationTestCase("M MMM", "MONTH_OF_YEAR"),
        ValidationTestCase("M MMMM", "MONTH_OF_YEAR"),
        ValidationTestCase("MM M", "MONTH_OF_YEAR"),
        ValidationTestCase("MM MM", "MONTH_OF_YEAR"),
        ValidationTestCase("MM MMM", "MONTH_OF_YEAR"),
        ValidationTestCase("MM MMMM", "MONTH_OF_YEAR"),
        ValidationTestCase("MMM M", "MONTH_OF_YEAR"),
        ValidationTestCase("MMM MM", "MONTH_OF_YEAR"),
        ValidationTestCase("MMM MMM", "MONTH_OF_YEAR"),
        ValidationTestCase("MMM MMMM", "MONTH_OF_YEAR"),
        ValidationTestCase("MMMM M", "MONTH_OF_YEAR"),
        ValidationTestCase("MMMM MM", "MONTH_OF_YEAR"),
        ValidationTestCase("MMMM MMM", "MONTH_OF_YEAR"),
        ValidationTestCase("MMMM MMMM", "MONTH_OF_YEAR"),

        // d and dd
        ValidationTestCase("d d", "DAY_OF_MONTH"),
        ValidationTestCase("d dd", "DAY_OF_MONTH"),
        ValidationTestCase("dd dd", "DAY_OF_MONTH"),

        // h, hh, H and HH
        ValidationTestCase("h h", "HOUR_OF_DAY"),
        ValidationTestCase("h hh", "HOUR_OF_DAY"),
        ValidationTestCase("hh hh", "HOUR_OF_DAY"),
        ValidationTestCase("H H", "HOUR_OF_DAY"),
        ValidationTestCase("H HH", "HOUR_OF_DAY"),
        ValidationTestCase("HH HH", "HOUR_OF_DAY"),
        ValidationTestCase("h H", "HOUR_OF_DAY"),
        ValidationTestCase("hh HH", "HOUR_OF_DAY"),

        // m and mm
        ValidationTestCase("m m", "MINUTE_OF_HOUR"),
        ValidationTestCase("m mm", "MINUTE_OF_HOUR"),
        ValidationTestCase("mm mm", "MINUTE_OF_HOUR"),

        // s and s
        ValidationTestCase("s s", "SECOND_OF_MINUTE"),
        ValidationTestCase("s ss", "SECOND_OF_MINUTE"),
        ValidationTestCase("ss ss", "SECOND_OF_MINUTE"),

        // n, S and S
        ValidationTestCase("n S", "FRACTION_OF_SECOND"),
        ValidationTestCase("n SS", "FRACTION_OF_SECOND"),
        ValidationTestCase("S SS", "FRACTION_OF_SECOND"),
        ValidationTestCase("S SSS", "FRACTION_OF_SECOND"),
        ValidationTestCase("S SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS", "FRACTION_OF_SECOND"),

        // x, xx, xxx, xxxx, and xxxxx (y is needed to prevent validation error from a different rule)
        ValidationTestCase("y x x", "OFFSET"),
        ValidationTestCase("y x xx", "OFFSET"),
        ValidationTestCase("y x xxx", "OFFSET"),
        ValidationTestCase("y x xxxx", "OFFSET"),
        ValidationTestCase("y x xxxxx", "OFFSET"),
        ValidationTestCase("y xx x", "OFFSET"),
        ValidationTestCase("y xx xx", "OFFSET"),
        ValidationTestCase("y xx xxx", "OFFSET"),
        ValidationTestCase("y xx xxxx", "OFFSET"),
        ValidationTestCase("y xx xxxxx", "OFFSET"),
        ValidationTestCase("y xxx x", "OFFSET"),
        ValidationTestCase("y xxx xx", "OFFSET"),
        ValidationTestCase("y xxx xxx", "OFFSET"),
        ValidationTestCase("y xxx xxxx", "OFFSET"),
        ValidationTestCase("y xxx xxxxx", "OFFSET"),
        ValidationTestCase("y xxxx x", "OFFSET"),
        ValidationTestCase("y xxxx xx", "OFFSET"),
        ValidationTestCase("y xxxx xxx", "OFFSET"),
        ValidationTestCase("y xxxx xxxx", "OFFSET"),
        ValidationTestCase("y xxxx xxxxx", "OFFSET"),
        ValidationTestCase("y xxxxx x", "OFFSET"),
        ValidationTestCase("y xxxxx xx", "OFFSET"),
        ValidationTestCase("y xxxxx xxx", "OFFSET"),
        ValidationTestCase("y xxxxx xxxx", "OFFSET"),
        ValidationTestCase("y xxxxx xxxxx", "OFFSET"),

        // X, XX, XXX, XXXX and XXXXX
        ValidationTestCase("y X X", "OFFSET"),
        ValidationTestCase("y X XX", "OFFSET"),
        ValidationTestCase("y X XXX", "OFFSET"),
        ValidationTestCase("y X XXXX", "OFFSET"),
        ValidationTestCase("y X XXXXX", "OFFSET"),
        ValidationTestCase("y XX X", "OFFSET"),
        ValidationTestCase("y XX XX", "OFFSET"),
        ValidationTestCase("y XX XXX", "OFFSET"),
        ValidationTestCase("y XX XXXX", "OFFSET"),
        ValidationTestCase("y XX XXXXX", "OFFSET"),
        ValidationTestCase("y XXX X", "OFFSET"),
        ValidationTestCase("y XXX XX", "OFFSET"),
        ValidationTestCase("y XXX XXX", "OFFSET"),
        ValidationTestCase("y XXX XXXX", "OFFSET"),
        ValidationTestCase("y XXX XXXXX", "OFFSET"),
        ValidationTestCase("y XXXX X", "OFFSET"),
        ValidationTestCase("y XXXX XX", "OFFSET"),
        ValidationTestCase("y XXXX XXX", "OFFSET"),
        ValidationTestCase("y XXXX XXXX", "OFFSET"),
        ValidationTestCase("y XXXX XXXXX", "OFFSET"),
        ValidationTestCase("y XXXXX X", "OFFSET"),
        ValidationTestCase("y XXXXX XX", "OFFSET"),
        ValidationTestCase("y XXXXX XXX", "OFFSET"),
        ValidationTestCase("y XXXXX XXXX", "OFFSET"),
        ValidationTestCase("y XXXXX XXXXX", "OFFSET"),

        // x and X (mixed case)
        ValidationTestCase("y x X", "OFFSET"),
        ValidationTestCase("y x XX", "OFFSET"),
        ValidationTestCase("y x XXX", "OFFSET"),
        ValidationTestCase("y x XXXX", "OFFSET"),
        ValidationTestCase("y x XXXXX", "OFFSET"),
        ValidationTestCase("y xx X", "OFFSET"),
        ValidationTestCase("y xx XX", "OFFSET"),
        ValidationTestCase("y xx XXX", "OFFSET"),
        ValidationTestCase("y xx XXXX", "OFFSET"),
        ValidationTestCase("y xx XXXXX", "OFFSET"),
        ValidationTestCase("y xxx X", "OFFSET"),
        ValidationTestCase("y xxx XX", "OFFSET"),
        ValidationTestCase("y xxx XXX", "OFFSET"),
        ValidationTestCase("y xxx XXXX", "OFFSET"),
        ValidationTestCase("y xxx XXXXX", "OFFSET"),
        ValidationTestCase("y xxxx X", "OFFSET"),
        ValidationTestCase("y xxxx XX", "OFFSET"),
        ValidationTestCase("y xxxx XXX", "OFFSET"),
        ValidationTestCase("y xxxx XXXX", "OFFSET"),
        ValidationTestCase("y xxxx XXXXX", "OFFSET"),
        ValidationTestCase("y xxxxx X", "OFFSET"),
        ValidationTestCase("y xxxxx XX", "OFFSET"),
        ValidationTestCase("y xxxxx XXX", "OFFSET"),
        ValidationTestCase("y xxxxx XXXX", "OFFSET"),
        ValidationTestCase("y xxxxx XXXXX", "OFFSET")
    )

    @Test
    @Parameters
    fun duplicateFieldPatternTest(testCase: ValidationTestCase) {
        checkInputThrowingEvaluationException(
            "TO_TIMESTAMP('doesnt matter', '${testCase.pattern}')",
            ErrorCode.EVALUATOR_TIMESTAMP_FORMAT_PATTERN_DUPLICATE_FIELDS,
            sourceLocationProperties(1, 1) + mapOf(
                Property.TIMESTAMP_FORMAT_PATTERN to testCase.pattern,
                Property.TIMESTAMP_FORMAT_PATTERN_FIELDS to testCase.fields
            ),
            expectedPermissiveModeResult = "MISSING"
        )
    }
}

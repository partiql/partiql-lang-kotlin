package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.partiql.eval.Mode
import org.partiql.spi.errors.PError
import org.partiql.spi.value.Datum
import java.math.BigDecimal

/**
 * An exact numeric literal keeps every digit, also past DECIMAL's default precision of 38 (SQL:2023 §5.3 Syntax
 * Rule 29). It used to be rounded to 38 digits.
 */
class ExactLiteralTest {

    private val digits = "1234567890123456789012345678901234567890.5" // 41 digits, scale 1

    @Test
    fun exactLiteralPast38DigitsKeepsEveryDigit() {
        SuccessTestCase(
            input = digits,
            mode = Mode.STRICT(),
            expected = Datum.decimal(BigDecimal(digits), 41, 1),
            jvmEquality = true,
        ).run()
    }

    @Test
    fun exactLiteralWithin38DigitsIsUnchanged() {
        SuccessTestCase(
            input = "10.5",
            mode = Mode.STRICT(),
            expected = Datum.decimal(BigDecimal("10.5"), 3, 1),
            jvmEquality = true,
        ).run()
    }

    // ABS has the type of its input (SQL:2023 §6.31 Syntax Rule 11). It used to report DECIMAL(38, 0).
    @Test
    fun absKeepsTheTypeOfItsInput() {
        SuccessTestCase(
            input = "ABS(-$digits)",
            mode = Mode.STRICT(),
            expected = Datum.decimal(BigDecimal(digits), 41, 1),
            jvmEquality = true,
        ).run()
    }

    // A sum past 38 digits is out of range, as for `+` (SQL:2023 §10.9 General Rule 7.d.vi). It used to be rounded.
    @Test
    fun sumPast38DigitsIsOutOfRange() {
        FailureTestCase(
            name = "out of range",
            input = "SELECT VALUE SUM(v) FROM <<$digits, 0>> AS v",
            expectedCode = PError.NUMERIC_VALUE_OUT_OF_RANGE,
        ).run()
    }

    @Test
    fun avgPast38DigitsIsOutOfRange() {
        FailureTestCase(
            name = "out of range",
            input = "SELECT VALUE AVG(v) FROM <<$digits, $digits>> AS v",
            expectedCode = PError.NUMERIC_VALUE_OUT_OF_RANGE,
        ).run()
    }

    @Test
    fun sumWithin38DigitsIsExact() {
        SuccessTestCase(
            input = "SELECT VALUE SUM(v) FROM <<12345678901234567890123456789012345.5, 0.25>> AS v",
            mode = Mode.STRICT(),
            expected = Datum.bagVararg(Datum.decimal(BigDecimal("12345678901234567890123456789012345.75"), 37, 2)),
        ).run()
    }
}

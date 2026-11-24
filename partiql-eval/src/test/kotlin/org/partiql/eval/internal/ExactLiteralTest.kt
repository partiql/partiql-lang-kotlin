package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum
import java.math.BigDecimal

/**
 * An exact numeric literal keeps every digit, also past DECIMAL's default precision of 38 (SQL:2023 §5.3 Syntax
 * Rule 29). It used to be rounded to 38 digits.
 */
class ExactLiteralTest {

    @Test
    fun exactLiteralPast38DigitsKeepsEveryDigit() {
        val digits = "1234567890123456789012345678901234567890.5" // 41 digits, scale 1
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
}

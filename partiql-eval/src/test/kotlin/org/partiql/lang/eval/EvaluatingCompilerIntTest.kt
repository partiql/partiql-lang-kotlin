package org.partiql.lang.eval

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.util.propertyValueMapOf
import java.math.BigInteger

/**
 * This class tests evaluation-time behavior for integer and integer overflows that existed *prior* to the
 * introduction of [StaticType].  The behavior described in these tests is still how the we should handle
 * integer arithmetic in the absence of [StaticType] information.
 */
class EvaluatingCompilerIntTest : EvaluatorTestBase() {

    @ParameterizedTest
    @MethodSource("parametersForValues")
    fun values(pair: Pair<String, String>) = assertPair(pair)

    @Test
    fun bigInt() = runEvaluatorErrorTestCase(
        "$bigInt",
        ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 1)
    )

    @Test
    fun negativeBigInt() = runEvaluatorErrorTestCase(
        "$negativeBigInt",
        ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 2)
    )

    // EMPTY
    // @ParameterizedTest
    // @MethodSource("parametersForPlus")
    // fun plus(pair: Pair<String, String>) = assertPair(pair)

    @Test
    fun plusOverflow() = runEvaluatorErrorTestCase(
        "$closeToMaxLong + $closeToMaxLong",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 21, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    // EMPTY
    // @ParameterizedTest
    // @MethodSource("parametersForMinus")
    // fun minus(pair: Pair<String, String>) = assertPair(pair)

    @Test
    fun minusUnderflow() = runEvaluatorErrorTestCase(
        "$closeToMinLong - $closeToMaxLong",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 22, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @ParameterizedTest
    @MethodSource("parametersForTimes")
    fun times(pair: Pair<String, String>) = assertPair(pair)

    @Test
    fun timesOverflow() = runEvaluatorErrorTestCase(
        "$closeToMaxLong * 2",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 21, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun timesUnderflow() = runEvaluatorErrorTestCase(
        "${Long.MIN_VALUE} * -1",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 22, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @ParameterizedTest
    @MethodSource("parametersForDivision")
    fun division(pair: Pair<String, String>) = assertPair(pair)

    @Test
    fun divisionUnderflow() = runEvaluatorErrorTestCase(
        "${Long.MIN_VALUE} / -1",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 22, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @ParameterizedTest
    @MethodSource("parametersForBitwiseAnd")
    fun bitwiseAnd(pair: Pair<String, String>) = assertPair(pair)

    @Test
    fun castBigInt() = runEvaluatorErrorTestCase(
        "cast('$bigInt' as int)",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun castNegativeBigInt() = runEvaluatorErrorTestCase(
        "cast('$negativeBigInt' as int)",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun castSmallDecimalExact() = runEvaluatorTestCase("cast(5e0 as int)", expectedResult = "5")

    @Test
    fun castSmallDecimal() = runEvaluatorTestCase("cast(5.2 as int)", expectedResult = "5")

    @Test
    fun castHugeDecimal() = runEvaluatorErrorTestCase(
        "cast(1e2147483609 as int)",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    fun castHugeNegativeDecimal() = runEvaluatorErrorTestCase(
        "cast(-1e2147483609 as int)",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 1, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    // TODO: see https://github.com/partiql/partiql-lang-kotlin/issues/784
//    @Test
//    fun castAlmostZeroDecimal() = runEvaluatorTestCase("cast(1e-2147483609 as int)", expectedResult = "0")

    @Test
    fun castAlmostOneDecimal() =
        runEvaluatorTestCase("cast((1.0 + 1e-2147483609) as int)", expectedResult = "1")

    private fun assertPair(pair: Pair<String, String>) {
        val (query, expected) = pair
        runEvaluatorTestCase(query, expectedResult = expected)
    }

    companion object {

        private val closeToMaxLong = (Long.MAX_VALUE - 1)
        private val closeToMinLong = (Long.MIN_VALUE + 1)

        private val bigInt = BigInteger.valueOf(Long.MAX_VALUE).times(BigInteger.valueOf(2))
        private val negativeBigInt = BigInteger.valueOf(Long.MIN_VALUE).times(BigInteger.valueOf(2))

        @JvmStatic
        fun parametersForValues(): List<Pair<String, String>> = listOf(
            "$closeToMaxLong" to "$closeToMaxLong",
            "$closeToMinLong" to "$closeToMinLong",
            "`0x00ffFFffFFffFFff`" to "72057594037927935",
        )

        // Deliberately kept in case we need to manually add test case in the future.
        @JvmStatic
        fun parametersForPlus(): List<Pair<String, String>> = emptyList()

        @JvmStatic
        fun parametersForTimes(): List<Pair<String, String>> = listOf(
            "${Long.MAX_VALUE} * -1" to "-${Long.MAX_VALUE}"
        )

        @JvmStatic
        fun parametersForDivision(): List<Pair<String, String>> = listOf(
            "${Long.MAX_VALUE} / -1" to "-${Long.MAX_VALUE}",
        )

        // Deliberately kept in case we need to manually add test case in the future.
        @JvmStatic
        fun parametersForMinus(): List<Pair<String, String>> = emptyList()

        @JvmStatic
        fun parametersForBitwiseAnd(): List<Pair<String, String>> = listOf(
            "1 & 2" to "0",
            "3 & 5" to "1",
            "5 & 7" to "5",
            "31 & 15 & 7 & 3 & 1" to "1",
            "1 + 5 & 5" to "4",
            "(1 + 5) & 5" to "4",
            "1 + (5 & 5)" to "6",
            "5 & 5 + 1" to "4",
            "(5 & 5) + 1" to "6",
            "5 & (5 + 1)" to "4",
        )
    }
}

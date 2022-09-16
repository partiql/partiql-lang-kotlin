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

package org.partiql.lang.eval

import junitparams.Parameters
import org.junit.BeforeClass
import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.propertyValueMapOf
import java.math.BigInteger
import java.util.Random

/**
 * This class tests evaluation-time behavior for integer and integer overflows that existed *prior* to the
 * introduction of [StaticType].  The behavior described in these tests is still how the we should handle
 * integer arithmetic in the absence of [StaticType] information.
 */
class EvaluatingCompilerIntTest : EvaluatorTestBase() {
    companion object {
        private val RANDOM = Random()

        @JvmStatic
        @BeforeClass
        fun setupRandomSeed() {
            val seed = System.nanoTime()

            println("IntTest seed = $seed, use it to reproduce failures")

            RANDOM.setSeed(seed)
        }
    }

    private val closeToMaxLong = (Long.MAX_VALUE - 1)
    private val closeToMinLong = (Long.MIN_VALUE + 1)

    private val bigInt = BigInteger.valueOf(Long.MAX_VALUE).times(BigInteger.valueOf(2))
    private val negativeBigInt = BigInteger.valueOf(Long.MIN_VALUE).times(BigInteger.valueOf(2))

    @Test
    @Parameters
    fun values(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForValues(): List<Pair<String, String>> {
        val transform: (Number) -> Pair<String, String> = { i -> "$i" to "$i" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map { RANDOM.nextInt() }.mapTo(parameters, transform)
        (1..20).map { RANDOM.nextLong() }.mapTo(parameters, transform)

        // defined manually
        parameters.add("$closeToMaxLong" to "$closeToMaxLong")
        parameters.add("$closeToMinLong" to "$closeToMinLong")
        parameters.add("`0x00ffFFffFFffFFff`" to "72057594037927935")

        return parameters
    }

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

    @Test
    @Parameters
    fun plus(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForPlus(): List<Pair<String, String>> {
        val transform: (Triple<Long, Long, Long>) -> Pair<String, String> =
            { (left, right, result) -> "$left + $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map {
            // generating an integer to ensure addition won't overflow
            val left = RANDOM.nextInt().toLong()
            val right = RANDOM.nextInt().toLong()

            Triple(left, right, left + right)
        }.mapTo(parameters, transform)

        return parameters
    }

    @Test
    fun plusOverflow() = runEvaluatorErrorTestCase(
        "$closeToMaxLong + $closeToMaxLong",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 21, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    @Parameters
    fun minus(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForMinus(): List<Pair<String, String>> {
        val transform: (Triple<Long, Long, Long>) -> Pair<String, String> =
            { (left, right, result) -> "$left - $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map {
            // generating an integer to ensure addition won't overflow
            val left = RANDOM.nextInt().toLong()
            val right = RANDOM.nextInt().toLong()

            Triple(left, right, left - right)
        }.mapTo(parameters, transform)

        return parameters
    }

    @Test
    fun minusUnderflow() = runEvaluatorErrorTestCase(
        "$closeToMinLong - $closeToMaxLong",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 22, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

    @Test
    @Parameters
    fun times(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForTimes(): List<Pair<String, String>> {
        val transform: (Triple<Long, Long, Long>) -> Pair<String, String> =
            { (left, right, result) -> "$left * $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..40).map { i ->
            var left = RANDOM.nextInt(1_000).toLong()
            if (i % 2 == 0) left = -left

            val right = RANDOM.nextInt(1_000).toLong()

            Triple(left, right, left * right)
        }.mapTo(parameters, transform)

        parameters.add("${Long.MAX_VALUE} * -1" to "-${Long.MAX_VALUE}")

        return parameters
    }

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

    @Test
    @Parameters
    fun division(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForDivision(): List<Pair<String, String>> {
        val transform: (Triple<Long, Long, Long>) -> Pair<String, String> =
            { (left, right, result) -> "$left / $right" to "$result" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..40).map { i ->
            var left = RANDOM.nextInt(1_000).toLong()
            if (i % 2 == 0) left = -left

            val right = RANDOM.nextInt(1_000).toLong() + 1 // to avoid being 0

            Triple(left, right, left / right)
        }.mapTo(parameters, transform)

        parameters.add("${Long.MAX_VALUE} / -1" to "-${Long.MAX_VALUE}")

        return parameters
    }

    @Test
    fun divisionUnderflow() = runEvaluatorErrorTestCase(
        "${Long.MIN_VALUE} / -1",
        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
        expectedErrorContext = propertyValueMapOf(1, 22, Property.INT_SIZE_IN_BYTES to 8),
        expectedPermissiveModeResult = "MISSING"
    )

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

    // TODO: Are we going to support extremely small decimal operations? If so, uncomment the next test and fix the bug
//    @Test
//    fun castAlmostZeroDecimal() = runEvaluatorTestCase("cast(1e-2147483609 as int)", expectedResult = "0")

    @Test
    fun castAlmostOneDecimal() =
        runEvaluatorTestCase("cast((1.0 + 1e-2147483609) as int)", expectedResult = "1")

    private fun assertPair(pair: Pair<String, String>) {
        val (query, expected) = pair
        runEvaluatorTestCase(query, expectedResult = expected)
    }
}

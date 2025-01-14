/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.lang.randomized.eval

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Random

/**
 * This class tests evaluation-time behavior for integer and integer overflows that existed *prior* to the
 * introduction of StaticType. The behavior described in these tests is still how we should handle integer arithmetic
 * in the absence of type information.
 *
 * TODO these tests are not correct and the implementation is not correct. The tests and implementation need to give
 *  an error when overflow does occur. See https://github.com/partiql/partiql-lang-kotlin/issues/1697.
 */
class IntOverflowRandomizedTest {
    companion object {
        private val RANDOM = Random()

        @JvmStatic
        @BeforeAll
        fun setupRandomSeed() {
            val seed = System.nanoTime()

            println("IntTest seed = $seed, use it to reproduce failures")

            RANDOM.setSeed(seed)
        }

        @JvmStatic
        fun parametersForValues(): List<Pair<String, String>> {
            val transform: (Number) -> Pair<String, String> = { i -> "$i" to "$i" }

            val parameters = mutableListOf<Pair<String, String>>()

            (1..20).map { RANDOM.nextInt() }.mapTo(parameters, transform)
            (1..20).map { RANDOM.nextLong() }.mapTo(parameters, transform)

            return parameters
        }

        @JvmStatic
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

        @JvmStatic
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

        @JvmStatic
        fun parametersForTimes(): List<Pair<String, String>> {
            val transform: (Triple<Long, Long, Long>) -> Pair<String, String> =
                { (left, right, result) -> "$left * $right" to "$result" }

            val parameters = mutableListOf<Pair<String, String>>()

            (1..40).map { i ->
                var left = RANDOM.nextInt(1_000).toLong() // TODO bound should be removed to allow for overflow
                if (i % 2 == 0) left = -left

                val right = RANDOM.nextInt(1_000).toLong() // TODO bound should be removed to allow for overflow

                Triple(left, right, left * right)
            }.mapTo(parameters, transform)

            return parameters
        }

        @JvmStatic
        fun parametersForDivision(): List<Pair<String, String>> {
            val transform: (Triple<Long, Long, Long>) -> Pair<String, String> =
                { (left, right, result) -> "$left / $right" to "$result" }

            val parameters = mutableListOf<Pair<String, String>>()

            (1..40).map { i ->
                var left = RANDOM.nextInt(1_000).toLong() // TODO bound should be removed to allow for overflow
                if (i % 2 == 0) left = -left

                // TODO bound should be removed to allow for overflow
                val right = RANDOM.nextInt(1_000).toLong() + 1 // to avoid being 0

                Triple(left, right, left / right)
            }.mapTo(parameters, transform)

            parameters.add("${Long.MAX_VALUE} / -1" to "-${Long.MAX_VALUE}")

            return parameters
        }
    }

    @ParameterizedTest
    @MethodSource("parametersForValues")
    @Disabled("The new execution engine and tests do not return the correct result. It should overflow.")
    fun values(pair: Pair<String, String>) = assertPair(pair)

    @ParameterizedTest
    @MethodSource("parametersForPlus")
    @Disabled("The new execution engine and tests do not return the correct result. It should overflow.")
    fun plus(pair: Pair<String, String>) = assertPair(pair)

    @ParameterizedTest
    @MethodSource("parametersForMinus")
    @Disabled("The new execution engine and tests do not return the correct result. It should overflow.")
    fun minus(pair: Pair<String, String>) = assertPair(pair)

    @ParameterizedTest
    @MethodSource("parametersForTimes")
    @Disabled("The new execution engine and tests do not return the correct result. It should overflow.")
    fun times(pair: Pair<String, String>) = assertPair(pair)

    @ParameterizedTest
    @MethodSource("parametersForDivision")
    @Disabled("The new execution engine and tests do not return the correct result. It should overflow.")
    fun division(pair: Pair<String, String>) = assertPair(pair)

    private fun assertPair(pair: Pair<String, String>) {
        val (query, expected) = pair
        runEvaluatorTestCase(query, expectedResult = expected)
    }
}

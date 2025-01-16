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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Random

/**
 * Randomized 4-byte integer arithmetic tests (+, -, *, /).
 *
 * Operations that would result in overflow expect an error. Operations that do not result in overflow check that the
 * operation output is correct.
 */
class IntOverflowRandomizedTest {
    sealed class Test {
        class NoOverflow(val query: String, val expected: String) : Test() {
            override fun toString(): String {
                return "$query = $expected"
            }
        }

        class Overflow(val query: String) : Test() {
            override fun toString(): String {
                return "Expect $query to overflow"
            }
        }
    }

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
        fun parametersForValues(): List<Test> {
            // No overflow on constructed values
            val transform: (Number) -> Test = { i -> Test.NoOverflow("$i", "$i") }

            val parameters = mutableListOf<Test>()

            (1..20).map { RANDOM.nextInt() }.mapTo(parameters, transform)
            (1..20).map { RANDOM.nextLong() }.mapTo(parameters, transform)

            return parameters
        }

        @JvmStatic
        fun parametersForPlus(): List<Test> {
            val transform: (Pair<Int, Int>) -> Test = { (left, right) ->
                try {
                    val result = Math.addExact(left, right)
                    Test.NoOverflow("CAST($left AS INT) + CAST($right AS INT)", "$result")
                } catch (e: ArithmeticException) {
                    Test.Overflow("$left + $right")
                }
            }

            val parameters = mutableListOf<Test>()

            (1..20).map {
                // generating an integer to ensure addition won't overflow
                val left = RANDOM.nextInt()
                val right = RANDOM.nextInt()

                Pair(left, right)
            }.mapTo(parameters, transform)

            return parameters
        }

        @JvmStatic
        fun parametersForMinus(): List<Test> {
            val transform: (Pair<Int, Int>) -> Test = { (left, right) ->
                try {
                    val result = Math.subtractExact(left, right)
                    Test.NoOverflow("CAST($left AS INT) - CAST($right AS INT)", "$result")
                } catch (e: ArithmeticException) {
                    Test.Overflow("$left - $right")
                }
            }

            val parameters = mutableListOf<Test>()

            (1..20).map {
                // generating an integer to ensure addition won't overflow
                val left = RANDOM.nextInt()
                val right = RANDOM.nextInt()

                Pair(left, right)
            }.mapTo(parameters, transform)

            return parameters
        }

        @JvmStatic
        fun parametersForTimes(): List<Test> {
            val transform: (Pair<Int, Int>) -> Test = { (left, right) ->
                try {
                    val result = Math.multiplyExact(left, right)
                    Test.NoOverflow("CAST($left AS INT) * CAST($right AS INT)", "$result")
                } catch (e: ArithmeticException) {
                    Test.Overflow("$left * $right")
                }
            }

            val parameters = mutableListOf<Test>()

            (1..40).map { i ->
                var left = RANDOM.nextInt()
                if (i % 2 == 0) left = -left

                val right = RANDOM.nextInt()

                Pair(left, right)
            }.mapTo(parameters, transform)

            return parameters
        }

        @JvmStatic
        fun parametersForDivision(): List<Test> {
            val transform: (Pair<Int, Int>) -> Test = { (left, right) ->
                if (left != Int.MIN_VALUE && right != -1) {
                    val result = left / right
                    Test.NoOverflow("CAST($left AS INT) / CAST($right AS INT)", "$result")
                } else {
                    Test.Overflow("$left / $right")
                }
            }

            val parameters = mutableListOf<Test>()

            (1..40).map { i ->
                var left = RANDOM.nextInt()
                if (i % 2 == 0) left = -left

                val right = RANDOM.nextInt() + 1

                Pair(left, right)
            }.mapTo(parameters, transform)

            // Guaranteed to overflow
            parameters.add(Test.Overflow("CAST(${Int.MIN_VALUE} AS INT) / CAST(-1 AS INT)"))
            return parameters
        }
    }

    @ParameterizedTest
    @MethodSource("parametersForValues")
    fun values(t: Test) = assertTest(t)

    @ParameterizedTest
    @MethodSource("parametersForPlus")
    fun plus(t: Test) = assertTest(t)

    @ParameterizedTest
    @MethodSource("parametersForMinus")
    fun minus(t: Test) = assertTest(t)

    @ParameterizedTest
    @MethodSource("parametersForTimes")
    fun times(t: Test) = assertTest(t)

    @ParameterizedTest
    @MethodSource("parametersForDivision")
    fun division(t: Test) = assertTest(t)

    private fun assertTest(pair: Test) {
        when (pair) {
            is Test.NoOverflow -> runEvaluatorTestCaseSuccess(pair.query, expectedResult = pair.expected)
            is Test.Overflow -> runEvaluatorTestCaseFailure(pair.query)
        }
    }
}

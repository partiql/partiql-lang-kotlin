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
import java.util.Random

/**
 * This class tests evaluation-time behavior for integer and integer overflows that existed *prior* to the
 * introduction of [StaticType].  The behavior described in these tests is still how the we should handle
 * integer arithmetic in the absence of [StaticType] information.
 */
class EvaluatingCompilerIntFuzzTest : EvaluatorTestBase() {
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

    @Test
    @Parameters
    fun values(pair: Pair<String, String>) = assertPair(pair)

    fun parametersForValues(): List<Pair<String, String>> {
        val transform: (Number) -> Pair<String, String> = { i -> "$i" to "$i" }

        val parameters = mutableListOf<Pair<String, String>>()

        (1..20).map { RANDOM.nextInt() }.mapTo(parameters, transform)
        (1..20).map { RANDOM.nextLong() }.mapTo(parameters, transform)

        return parameters
    }

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

        return parameters
    }

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

    private fun assertPair(pair: Pair<String, String>) {
        val (query, expected) = pair
        runEvaluatorTestCase(query, expectedResult = expected)
    }
}

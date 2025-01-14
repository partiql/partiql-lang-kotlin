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

package org.partiql.lang.randomized.syntax

import org.junit.jupiter.api.Test
import org.partiql.lang.randomized.eval.assertExpression
import org.partiql.spi.value.Datum
import java.time.LocalDate
import java.util.Random

class PartiQLParserDateTimeRandomizedTests {

    private data class Date(val year: Int, val month: Int, val day: Int)

    private val monthsWith31Days = listOf(1, 3, 5, 7, 8, 10, 12)
    private val randomGenerator = generateRandomSeed()
    private val randomDates = List(500) { randomGenerator.nextDate() }

    private fun generateRandomSeed(): Random {
        val rng = Random()
        val seed = rng.nextLong()
        println("Randomly generated seed is $seed.  Use this to reproduce failures in dev environment.")
        rng.setSeed(seed)
        return rng
    }

    private fun Random.nextDate(): Date {
        val year = nextInt(10000)
        val month = nextInt(12) + 1
        val day = when (month) {
            in monthsWith31Days -> nextInt(31)
            2 -> when ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                true -> nextInt(29)
                false -> nextInt(28)
            }
            else -> nextInt(30)
        } + 1
        return Date(year, month, day)
    }

    @Test
    fun testRandomDates() {
        randomDates.map { date ->
            val yearStr = date.year.toString().padStart(4, '0')
            val monthStr = date.month.toString().padStart(2, '0')
            val dayStr = date.day.toString().padStart(2, '0')
            assertExpression("DATE '$yearStr-$monthStr-$dayStr'") {
                Datum.date(LocalDate.of(date.year, date.month, date.day))
            }
        }
    }
}

package org.partiql.lang.syntax

import org.junit.Test
import java.util.Random

class PartiQLParserDateTimeFuzzTests : PartiQLParserTestBase() {

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
                date(date.year.toLong(), date.month.toLong(), date.day.toLong())
            }
        }
    }
}

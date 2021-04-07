package org.partiql.lang.syntax

import junitparams.Parameters
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id
import java.util.*

class SqlParserDateTimeTests : SqlParserTestBase() {

    data class DateTimeTestCase(val source: String, val block: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode)
    private data class Date(val year: Int, val month: Int, val day: Int)

    private val MONTHS_WITH_31_DAYS = listOf(1, 3, 5, 7, 8, 10, 12)
    private val RANDOM_GENERATOR = generateRandomSeed()
    private val RANDOM_DATES = List(500) { RANDOM_GENERATOR.nextDate() }

    @Test
    @Parameters
    fun dateLiteralTests(tc: DateTimeTestCase) = assertExpression(tc.source, tc.block)

    fun parametersForDateLiteralTests() = listOf(
        DateTimeTestCase("DATE '2012-02-29'") {
            date(2012, 2, 29)
        },
        DateTimeTestCase("DATE'1992-11-30'") {
            date(1992, 11, 30)
        },
        DateTimeTestCase("DATE '9999-03-01'") {
            date(9999, 3, 1)
        },
        DateTimeTestCase("DATE '0000-01-01'") {
            date(0, 1, 1)
        },
        DateTimeTestCase("DATE '0000-02-29'") {
            date(0, 2, 29)
        },
        DateTimeTestCase("DATE '0000-02-29'") {
            date(0, 2, 29)
        },
        DateTimeTestCase("SELECT DATE '2021-03-10' FROM foo") {
            select(
                project = projectList(projectExpr(date(2021, 3, 10))),
                from = scan(id("foo"))
            )
        }
    )

    private fun generateRandomSeed() : Random {
        val rng = Random()
        val seed = rng.nextLong()
        println("Randomly generated seed is ${seed}.  Use this to reproduce failures in dev environment.")
        rng.setSeed(seed)
        return rng
    }

    private fun Random.nextDate() : Date {
        val year = nextInt(10000)
        val month = nextInt(12) + 1
        val day = when (month) {
            in MONTHS_WITH_31_DAYS -> nextInt(31)
            2 -> when (year % 4) {
                0 -> nextInt(29)
                else -> nextInt(28)
            }
            else -> nextInt(30)
        } + 1
        return Date(year, month, day)
    }

    @Test
    fun testRandomDates() {
        RANDOM_DATES.map { date ->
            val yearStr = date.year.toString().padStart(4, '0')
            val monthStr = date.month.toString().padStart(2, '0')
            val dayStr = date.day.toString().padStart(2, '0')
            assertExpression("DATE '$yearStr-$monthStr-$dayStr'") {
                date(date.year.toLong(), date.month.toLong(), date.day.toLong())
            }
        }
    }
}
package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

/**
 * This test file tests Common Table Expressions.
 */
class WindowTests {

    @ParameterizedTest
    @MethodSource("successTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun successTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("failureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun failureTests(tc: FailureTestCase) = tc.run()

    companion object {

        private class Employee(
            val id: Int,
            val name: String,
            val department: String,
            val age: Int,
            val partner: String?
        ) {
            fun toDatum(): Datum {
                return Datum.struct(
                    Field.of("id", Datum.integer(id)),
                    Field.of("name", Datum.string(name)),
                    Field.of("department", Datum.string(department)),
                    Field.of("age", Datum.integer(age)),
                    Field.of("partner", partner?.let { Datum.string(it) } ?: Datum.missing(PType.string()))
                )
            }
        }

        private val employees = listOf(
            Employee(0, "Jacob", "Marketing", 40, "Alexa"),
            Employee(1, "Marcus", "Sales", 28, "Gary"),
            Employee(2, "Shelly", "Research", 35, "Michael"),
            Employee(3, "Alexa", "Research", 30, null),
            Employee(4, "Raghavan", "Sales", 28, "Yi"),
            Employee(5, "Yi", "Research", 25, "Raghavan"),
            Employee(6, "Megan", "Marketing", 32, null),
            Employee(7, "Amanda", "Research", 32, null),
            Employee(8, "Samantha", "Sales", 29, "Mason"),
            Employee(9, "Mason", "Research", 30, "Samantha")
        )

        private val globals = listOf(
            Global(
                name = "employee",
                value = Datum.bag(employees.map { it.toDatum() })
            )
        )

        private const val FALLBACK: String = "UNKNOWN"

        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Simplest window with all functions",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.name, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.name, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(6, 1, 1, 1, null, 0),
                    rowOf(0, 2, 2, 2, 6, null),
                    rowOf(5, 1, 1, 1, null, 3),
                    rowOf(3, 2, 2, 2, 5, 9),
                    rowOf(9, 2, 2, 3, 3, 7),
                    rowOf(7, 4, 3, 4, 9, 2),
                    rowOf(2, 5, 4, 5, 7, null),
                    rowOf(1, 1, 1, 1, null, 4),
                    rowOf(4, 1, 1, 2, 1, 8),
                    rowOf(8, 3, 2, 3, 4, null),
                ),
            ),
            SuccessTestCase(
                name = "Lead/Lag with more than 1",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.name, 3, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.name, 3, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(6, 1, 1, 1, null, null),
                    rowOf(0, 2, 2, 2, null, null),
                    rowOf(5, 1, 1, 1, null, 7),
                    rowOf(3, 2, 2, 2, null, 2),
                    rowOf(9, 2, 2, 3, null, null),
                    rowOf(7, 4, 3, 4, 5, null),
                    rowOf(2, 5, 4, 5, 3, null),
                    rowOf(1, 1, 1, 1, null, null),
                    rowOf(4, 1, 1, 2, null, null),
                    rowOf(8, 3, 2, 3, null, null),
                ),
            ),
            SuccessTestCase(
                name = "Lag and lead referencing sometimes missing attr (partner)",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.partner, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.partner, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfPartner(6, 1, 1, 1, FALLBACK, "Alexa"),
                    rowOfPartner(0, 2, 2, 2, null, FALLBACK),
                    rowOfPartner(5, 1, 1, 1, FALLBACK, null),
                    rowOfPartner(3, 2, 2, 2, "Raghavan", "Samantha"),
                    rowOfPartner(9, 2, 2, 3, null, null),
                    rowOfPartner(7, 4, 3, 4, "Samantha", "Michael"),
                    rowOfPartner(2, 5, 4, 5, null, FALLBACK),
                    rowOfPartner(1, 1, 1, 1, FALLBACK, "Yi"),
                    rowOfPartner(4, 1, 1, 2, "Gary", "Mason"),
                    rowOfPartner(8, 3, 2, 3, "Yi", FALLBACK),
                ),
            ),
            // TODO: Test where expr of lag/lead may result in MISSING in permissive mode
        )

        /**
         * @param id The employee's id
         * @param rank The employee's rank within their department
         * @param denseRank The employee's dense rank within their department
         * @param rowNumber The employee's row number within their department
         * @param lag The index of the employee's name from the previous row within their department
         * @param lead The index of the employee's name from the next row within their department
         */
        private fun rowOf(id: Int, rank: Long, denseRank: Long, rowNumber: Long, lag: Int?, lead: Int?): Datum {
            return Datum.struct(
                Field.of("_id", Datum.integer(id)),
                Field.of("_name", Datum.string(employees[id].name)),
                Field.of("_rank", Datum.bigint(rank)),
                Field.of("_dense_rank", Datum.bigint(denseRank)),
                Field.of("_row_number", Datum.bigint(rowNumber)),
                Field.of("_lag", Datum.string(lag?.let { employees[it].name } ?: FALLBACK)),
                Field.of("_lead", Datum.string(lead?.let { employees[it].name } ?: FALLBACK))
            )
        }

        /**
         * @param id The employee's id
         * @param rank The employee's rank within their department
         * @param denseRank The employee's dense rank within their department
         * @param rowNumber The employee's row number within their department
         * @param lag The index of the employee's partner from the previous row within their department
         * @param lead The index of the employee's partner from the next row within their department
         */
        private fun rowOfPartner(id: Int, rank: Long, denseRank: Long, rowNumber: Long, lag: String?, lead: String?): Datum {
            val fields = listOfNotNull(
                Field.of("_id", Datum.integer(id)),
                Field.of("_name", Datum.string(employees[id].name)),
                Field.of("_rank", Datum.bigint(rank)),
                Field.of("_dense_rank", Datum.bigint(denseRank)),
                Field.of("_row_number", Datum.bigint(rowNumber)),
                lag?.let { Field.of("_lag", Datum.string(it)) },
                lead?.let { Field.of("_lead", Datum.string(it)) },
            )
            return Datum.struct(fields)
        }

        @JvmStatic
        fun failureTestCases() = listOf(
            FailureTestCase(
                name = "Lag and lead referencing sometimes missing attr (partner)",
                mode = Mode.STRICT(),
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.partner, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.partner, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent()
            )
        )
    }
}

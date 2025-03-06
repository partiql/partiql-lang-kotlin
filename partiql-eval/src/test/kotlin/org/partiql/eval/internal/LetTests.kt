package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

/**
 * This test file exercises the `LET` clause in PartiQL.
 */
class LetTests {

    @ParameterizedTest
    @MethodSource("successTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun successTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("failureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun failureTests(tc: FailureTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Basic LET usage 1",
                input = """
                    SELECT t.a, c 
                    FROM <<{ 'a': 1 , 'b': 2}>> AS t 
                    LET t.a*5 AS c
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("c", Datum.integer(5))
                    )
                )
            ),

            SuccessTestCase(
                name = "Basic LET usage 2",
                input = """
                    SELECT t.x, t.y, t.z * 2 AS double_z
                    FROM (
                        SELECT A AS x, B AS y, new_val AS z
                        FROM <<{ 'A': 1, 'B': 2, 'C': 3}>> 
                        LET B + C AS new_val
                    ) AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("x", Datum.integer(1)),
                        Field.of("y", Datum.integer(2)),
                        Field.of("double_z", Datum.integer(10))
                    )
                )
            ),

            SuccessTestCase(
                name = "LET with JOIN operation",
                input = """
                    SELECT t.customer_name, t.order_total
                    FROM (
                        SELECT 
                            c.name AS customer_name,
                            total AS order_total
                        FROM <<
                            { 'id': 1, 'name': 'Alice' },
                            { 'id': 2, 'name': 'Bob' }
                        >> AS c
                        JOIN <<
                            { 'customer_id': 1, 'amount': 100 },
                            { 'customer_id': 1, 'amount': 200 },
                            { 'customer_id': 2, 'amount': 150 }
                        >> AS o
                        ON c.id = o.customer_id
                        LET o.amount * c.id AS total
                    ) AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("customer_name", Datum.string("Alice")),
                        Field.of("order_total", Datum.integer(100))
                    ),
                    Datum.struct(
                        Field.of("customer_name", Datum.string("Alice")),
                        Field.of("order_total", Datum.integer(200))
                    ),
                    Datum.struct(
                        Field.of("customer_name", Datum.string("Bob")),
                        Field.of("order_total", Datum.integer(300))
                    )
                )
            ),

            SuccessTestCase(
                name = "LET with multiple items in data",
                input = """
                    SELECT t.x, t.y, t.z AS total
                    FROM (
                        SELECT A AS x, B AS y, sum_val AS z
                        FROM << 
                            { 'A': 1, 'B': 2, 'C': 3 }, 
                            { 'A': 10, 'B': 20, 'C': 30 }
                        >>
                        LET B + C AS sum_val
                    ) AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("x", Datum.integer(1)),
                        Field.of("y", Datum.integer(2)),
                        Field.of("total", Datum.integer(5))
                    ),
                    Datum.struct(
                        Field.of("x", Datum.integer(10)),
                        Field.of("y", Datum.integer(20)),
                        Field.of("total", Datum.integer(50))
                    )
                )
            ),
            SuccessTestCase(
                name = "LET referencing prior expressions",
                input = """
                    SELECT t.x, t.sum_val, t.double_sum
                    FROM (
                        SELECT
                            A AS x,
                            sum_val,
                            sum_val * 2 AS double_sum
                        FROM <<
                            { 'A': 3, 'B': 5 },
                            { 'A': 10, 'B': 2 }
                        >>
                        LET A + B AS sum_val
                    ) AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("x", Datum.integer(3)),
                        Field.of("sum_val", Datum.integer(8)),
                        Field.of("double_sum", Datum.integer(16))
                    ),
                    Datum.struct(
                        Field.of("x", Datum.integer(10)),
                        Field.of("sum_val", Datum.integer(12)),
                        Field.of("double_sum", Datum.integer(24))
                    )
                )
            ),
            SuccessTestCase(
                name = "LET with multiple LET clauses",
                input = """
                    SELECT t.a, b, c 
                    FROM << { 'a': 1 }>> AS t 
                    LET t.a + 2 AS b, t.a * 3 AS c
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.integer(3)),
                        Field.of("c", Datum.integer(3))
                    )
                )
            )
        )

        @JvmStatic
        fun failureTestCases() = listOf(
            FailureTestCase(
                name = "LET referencing undefined variable",
                input = """
                    SELECT t.x
                    FROM (
                        SELECT A AS x
                        FROM << { 'A': 1, 'B': 2 } >>
                        LET nonexistent + B AS new_val
                    ) AS t;
                """.trimIndent()
            ),
            FailureTestCase(
                name = "LET clause with ambiguous reference",
                input = """
                    SELECT t.z
                    FROM (
                        SELECT new_val AS z
                        FROM << { 'A': 1, 'B': 2 } >>
                        -- 'new_val' references itself in LET, which is not allowed
                        LET new_val + B AS new_val
                    ) AS t;
                """.trimIndent()
            ),
            FailureTestCase(
                name = "Outside clauses referencing subquery's LET bindings",
                input = """
                    SELECT t.x, t.y, new_val 
                    FROM ( 
                        SELECT A AS x, B AS y 
                        FROM <<{ 'A': 1, 'B': 2, 'C': 3}>> 
                        LET B + C AS new_val
                        ) AS t;
                """.trimIndent()
            ),
            FailureTestCase(
                name = "LET with invalid JOIN reference",
                input = """
                        SELECT t.customer_name, t.calculated_total
                        FROM (
                            SELECT 
                                c.name AS customer_name,
                                total AS calculated_total
                            FROM <<
                                { 'id': 1, 'name': 'Alice' },
                                { 'id': 2, 'name': 'Bob' }
                            >> AS c
                            LEFT JOIN <<
                                { 'customer_id': 1, 'amount': 100 },
                                { 'customer_id': 2, 'amount': 150 }
                            >> AS o
                            ON c.id = o.customer_id
                            -- This should fail because we're trying to reference 'missing_field'
                            -- which doesn't exist in either joined table
                            LET missing_field + o.amount AS total
                        ) AS t;
                """.trimIndent(),
            )
        )
    }

    // Example of a test that might need special handling or a skip
    @Test
    @Disabled("Demonstration of a scenario needing further investigation.")
    fun disabledTestExample() {
        // Implementation left blank or used for demonstration
    }
}

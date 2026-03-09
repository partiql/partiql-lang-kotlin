package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum

/**
 * Comprehensive tests for IN collection predicate, covering both:
 * - Non-SQL-style: RHS is a literal bag/array (uses `in_collection`)
 * - SQL-style: RHS is a SQL SELECT subquery (uses `sql_in_collection`)
 */
class InCollectionTests {

    @ParameterizedTest
    @MethodSource("nonSqlInCollectionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun nonSqlInCollectionTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("sqlInCollectionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sqlInCollectionTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("selectValueInCollectionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun selectValueInCollectionTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("multiColumnSqlInCollectionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun multiColumnSqlInCollectionTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("inCollectionWithGlobalsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun inCollectionWithGlobalsTests(tc: SuccessTestCase) = tc.run()

    companion object {

        // =============================================================================
        // Non-SQL-style IN: RHS is a literal bag/array (uses in_collection)
        // =============================================================================
        @JvmStatic
        fun nonSqlInCollectionCases() = listOf(
            // Basic match — integer in bag
            SuccessTestCase(
                name = "IN literal bag — match",
                input = "1 IN <<1, 2, 3>>;",
                expected = Datum.bool(true)
            ),
            // Basic no match — integer not in bag
            SuccessTestCase(
                name = "IN literal bag — no match",
                input = "4 IN <<1, 2, 3>>;",
                expected = Datum.bool(false)
            ),
            // NOT IN — match means false
            SuccessTestCase(
                name = "NOT IN literal bag — value present",
                input = "1 NOT IN <<1, 2, 3>>;",
                expected = Datum.bool(false)
            ),
            // NOT IN — no match means true
            SuccessTestCase(
                name = "NOT IN literal bag — value absent",
                input = "4 NOT IN <<1, 2, 3>>;",
                expected = Datum.bool(true)
            ),
            // String match
            SuccessTestCase(
                name = "IN literal bag — string match",
                input = "'a' IN <<'a', 'b', 'c'>>;",
                expected = Datum.bool(true)
            ),
            // String no match
            SuccessTestCase(
                name = "IN literal bag — string no match",
                input = "'z' IN <<'a', 'b', 'c'>>;",
                expected = Datum.bool(false)
            ),
            // Empty collection
            SuccessTestCase(
                name = "IN empty bag",
                input = "1 IN <<>>;",
                expected = Datum.bool(false)
            ),
            // NOT IN empty collection
            SuccessTestCase(
                name = "NOT IN empty bag",
                input = "1 NOT IN <<>>;",
                expected = Datum.bool(true)
            ),
            // Mixed types — integer found among mixed
            SuccessTestCase(
                name = "IN literal bag — mixed types, match found",
                input = "1 IN <<1, 'a', true>>;",
                expected = Datum.bool(true)
            ),
            // Mixed types — no match
            SuccessTestCase(
                name = "IN literal bag — mixed types, no match",
                input = "99 IN <<1, 'a', true>>;",
                expected = Datum.bool(false)
            ),
            // Boolean match
            SuccessTestCase(
                name = "IN literal bag — boolean match",
                input = "true IN <<false, true>>;",
                expected = Datum.bool(true)
            ),
            // IN with array literal
            SuccessTestCase(
                name = "IN literal array — match",
                input = "2 IN [1, 2, 3];",
                expected = Datum.bool(true)
            ),
            // IN with array literal — no match
            SuccessTestCase(
                name = "IN literal array — no match",
                input = "5 IN [1, 2, 3];",
                expected = Datum.bool(false)
            ),
        )

        // =============================================================================
        // SQL-style IN: RHS is a SQL SELECT (SelectList/SelectStar) — uses sql_in_collection
        // =============================================================================
        @JvmStatic
        fun sqlInCollectionCases() = listOf(
            // Single-column SELECT — match found
            SuccessTestCase(
                name = "SQL IN — single column SELECT, match",
                input = "1 IN (SELECT t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(true)
            ),
            // Single-column SELECT — no match
            SuccessTestCase(
                name = "SQL IN — single column SELECT, no match",
                input = "5 IN (SELECT t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(false)
            ),
            // Single-column SELECT with NOT — match negated
            SuccessTestCase(
                name = "SQL NOT IN — single column SELECT, value present",
                input = "1 NOT IN (SELECT t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(false)
            ),
            // Single-column NOT IN — no match so true
            SuccessTestCase(
                name = "SQL NOT IN — single column SELECT, value absent",
                input = "5 NOT IN (SELECT t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(true)
            ),
            // SELECT * from single-column source — match
            SuccessTestCase(
                name = "SQL IN — SELECT *, single column, match",
                input = "1 IN (SELECT * FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(true)
            ),
            // SELECT * from single-column source — no match
            SuccessTestCase(
                name = "SQL IN — SELECT *, single column, no match",
                input = "5 IN (SELECT * FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(false)
            ),
            // Empty subquery result
            SuccessTestCase(
                name = "SQL IN — empty subquery",
                input = "1 IN (SELECT t.a FROM <<{'a': 1}>> AS t WHERE t.a > 100);",
                expected = Datum.bool(false)
            ),
            // NOT IN with empty subquery result
            SuccessTestCase(
                name = "SQL NOT IN — empty subquery",
                input = "1 NOT IN (SELECT t.a FROM <<{'a': 1}>> AS t WHERE t.a > 100);",
                expected = Datum.bool(true)
            ),
            // String values in subquery
            SuccessTestCase(
                name = "SQL IN — string values in SELECT",
                input = "'hello' IN (SELECT t.s FROM <<{'s': 'hello'}, {'s': 'world'}>> AS t);",
                expected = Datum.bool(true)
            ),
            // String not found in subquery
            SuccessTestCase(
                name = "SQL IN — string not found in SELECT",
                input = "'foo' IN (SELECT t.s FROM <<{'s': 'hello'}, {'s': 'world'}>> AS t);",
                expected = Datum.bool(false)
            ),
            // Filtered subquery — match
            SuccessTestCase(
                name = "SQL IN — filtered subquery, match",
                input = "2 IN (SELECT t.a FROM <<{'a': 1}, {'a': 2}, {'a': 3}>> AS t WHERE t.a >= 2);",
                expected = Datum.bool(true)
            ),
            // Filtered subquery — no match
            SuccessTestCase(
                name = "SQL IN — filtered subquery, no match",
                input = "1 IN (SELECT t.a FROM <<{'a': 1}, {'a': 2}, {'a': 3}>> AS t WHERE t.a >= 2);",
                expected = Datum.bool(false)
            ),
        )

        // =============================================================================
        // SELECT VALUE on RHS — NOT a SQL SELECT, so uses regular in_collection
        // =============================================================================
        @JvmStatic
        fun selectValueInCollectionCases() = listOf(
            // SELECT VALUE produces scalar values, not structs — uses in_collection
            SuccessTestCase(
                name = "IN with SELECT VALUE — match",
                input = "1 IN (SELECT VALUE t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                name = "IN with SELECT VALUE — no match",
                input = "5 IN (SELECT VALUE t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                name = "NOT IN with SELECT VALUE — match negated",
                input = "1 NOT IN (SELECT VALUE t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                name = "NOT IN with SELECT VALUE — value absent",
                input = "5 NOT IN (SELECT VALUE t.a FROM <<{'a': 1}, {'a': 2}>> AS t);",
                expected = Datum.bool(true)
            ),
            // SELECT VALUE with filter
            SuccessTestCase(
                name = "IN with SELECT VALUE — filtered, match",
                input = "2 IN (SELECT VALUE t.a FROM <<{'a': 1}, {'a': 2}, {'a': 3}>> AS t WHERE t.a >= 2);",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                name = "IN with SELECT VALUE — filtered, no match",
                input = "1 IN (SELECT VALUE t.a FROM <<{'a': 1}, {'a': 2}, {'a': 3}>> AS t WHERE t.a >= 2);",
                expected = Datum.bool(false)
            ),
            // SELECT VALUE with empty result
            SuccessTestCase(
                name = "IN with SELECT VALUE — empty result",
                input = "1 IN (SELECT VALUE t.a FROM <<{'a': 1}>> AS t WHERE t.a > 100);",
                expected = Datum.bool(false)
            ),
        )

        // =============================================================================
        // Multi-column SQL IN: LHS is SQL-style row value (parenthesized), RHS is multi-column SELECT
        // =============================================================================
        @JvmStatic
        fun multiColumnSqlInCollectionCases() = listOf(
            // Match on first row
            SuccessTestCase(
                name = "Multi-column SQL IN — match first row",
                input = "(1, 2) IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(true)
            ),
            // Match on second row
            SuccessTestCase(
                name = "Multi-column SQL IN — match second row",
                input = "(3, 4) IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(true)
            ),
            // Partial match — first col matches but second doesn't
            SuccessTestCase(
                name = "Multi-column SQL IN — partial match, no full row",
                input = "(1, 4) IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(false)
            ),
            // No match at all
            SuccessTestCase(
                name = "Multi-column SQL IN — no match",
                input = "(5, 6) IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(false)
            ),
            // NOT IN — match present
            SuccessTestCase(
                name = "Multi-column SQL NOT IN — value present",
                input = "(1, 2) NOT IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(false)
            ),
            // NOT IN — value absent
            SuccessTestCase(
                name = "Multi-column SQL NOT IN — value absent",
                input = "(5, 6) NOT IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(true)
            ),
            // Empty subquery
            SuccessTestCase(
                name = "Multi-column SQL IN — empty subquery",
                input = "(1, 2) IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}>> AS t WHERE t.a > 100);",
                expected = Datum.bool(false)
            ),
            // SELECT * multi-column — match
            SuccessTestCase(
                name = "Multi-column SQL IN — SELECT *, match",
                input = "(1, 2) IN (SELECT * FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(true)
            ),
            // SELECT * multi-column — no match
            SuccessTestCase(
                name = "Multi-column SQL IN — SELECT *, no match",
                input = "(9, 9) IN (SELECT * FROM <<{'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t);",
                expected = Datum.bool(false)
            ),
            // Scalar LHS with multi-column RHS — should not match (scalar vs struct with 2 fields)
            SuccessTestCase(
                name = "Multi-column SQL IN — scalar LHS, multi-column RHS, no match",
                input = "1 IN (SELECT t.a, t.b FROM <<{'a': 1, 'b': 2}>> AS t);",
                expected = Datum.bool(false)
            ),
            // Three columns — match
            SuccessTestCase(
                name = "Multi-column SQL IN — 3 columns, match",
                input = "(1, 2, 3) IN (SELECT t.a, t.b, t.c FROM <<{'a': 1, 'b': 2, 'c': 3}, {'a': 4, 'b': 5, 'c': 6}>> AS t);",
                expected = Datum.bool(true)
            ),
            // Three columns — no match
            SuccessTestCase(
                name = "Multi-column SQL IN — 3 columns, no match",
                input = "(1, 2, 6) IN (SELECT t.a, t.b, t.c FROM <<{'a': 1, 'b': 2, 'c': 3}, {'a': 4, 'b': 5, 'c': 6}>> AS t);",
                expected = Datum.bool(false)
            ),
        )

        // =============================================================================
        // IN with globals (table bindings)
        // =============================================================================
        @JvmStatic
        fun inCollectionWithGlobalsCases() = listOf(
            // SQL SELECT from global table — match
            SuccessTestCase(
                name = "SQL IN — global table, match",
                input = "1 IN (SELECT t.a FROM t);",
                expected = Datum.bool(true),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    )
                )
            ),
            // SQL SELECT from global table — no match
            SuccessTestCase(
                name = "SQL IN — global table, no match",
                input = "10 IN (SELECT t.a FROM t);",
                expected = Datum.bool(false),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    )
                )
            ),
            // SQL NOT IN from global table
            SuccessTestCase(
                name = "SQL NOT IN — global table, value present",
                input = "1 NOT IN (SELECT t.a FROM t);",
                expected = Datum.bool(false),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    )
                )
            ),
            // SELECT VALUE from global table — uses in_collection
            SuccessTestCase(
                name = "IN with SELECT VALUE — global table, match",
                input = "2 IN (SELECT VALUE t.a FROM t);",
                expected = Datum.bool(true),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    )
                )
            ),
            // SELECT VALUE from global table — no match
            SuccessTestCase(
                name = "IN with SELECT VALUE — global table, no match",
                input = "10 IN (SELECT VALUE t.a FROM t);",
                expected = Datum.bool(false),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    )
                )
            ),
            // IN used in WHERE clause with global
            SuccessTestCase(
                name = "SQL IN — used in WHERE clause with globals",
                input = """
                    SELECT VALUE t.a
                    FROM t
                    WHERE t.a IN (SELECT s.x FROM s);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.bigint(1),
                    Datum.bigint(3)
                ),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    ),
                    Global(
                        name = "s",
                        value = """[{"x": 1}, {"x": 3}, {"x": 5}]"""
                    )
                )
            ),
            // NOT IN used in WHERE clause with global
            SuccessTestCase(
                name = "SQL NOT IN — used in WHERE clause with globals",
                input = """
                    SELECT VALUE t.a
                    FROM t
                    WHERE t.a NOT IN (SELECT s.x FROM s);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.bigint(2)
                ),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
                    ),
                    Global(
                        name = "s",
                        value = """[{"x": 1}, {"x": 3}, {"x": 5}]"""
                    )
                )
            ),
        )
    }
}

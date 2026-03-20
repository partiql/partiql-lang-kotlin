package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

class SetOpTests {

    @ParameterizedTest
    @MethodSource("unionSchemaCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testUnion(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intersectSchemaCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIntersect(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("exceptSchemaCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExcept(tc: SuccessTestCase) = tc.run()

    // --- OUTER set op success cases (incompatible types allowed) ---
    @ParameterizedTest
    @MethodSource("outerUnionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOuterUnion(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("outerIntersectCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOuterIntersect(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("outerExceptCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOuterExcept(tc: SuccessTestCase) = tc.run()

    // --- NULL and MISSING cases ---
    @ParameterizedTest
    @MethodSource("unionNullMissingCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testUnionNullMissing(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intersectNullMissingCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIntersectNullMissing(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("exceptNullMissingCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExceptNullMissing(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun unionSchemaCases() = listOf(
            SuccessTestCase(
                name = "UNION INT and INT - distinct",
                input = "SELECT a FROM <<{'a': 1}>> UNION SELECT a FROM <<{'a': 1}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
            SuccessTestCase(
                name = "UNION INT and INT - different values",
                input = "SELECT a FROM <<{'a': 1}>> UNION SELECT a FROM <<{'a': 2}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "UNION ALL INT and INT - duplicates preserved",
                input = "SELECT a FROM <<{'a': 1}>> UNION ALL SELECT a FROM <<{'a': 1}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
            SuccessTestCase(
                name = "UNION STRING and STRING",
                input = "SELECT a FROM <<{'a': 'x'}>> UNION SELECT a FROM <<{'a': 'y'}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.string("x"))),
                    Datum.struct(Field.of("a", Datum.string("y")))
                )
            ),
            SuccessTestCase(
                name = "UNION BOOL and BOOL",
                input = "SELECT a FROM <<{'a': TRUE}>> UNION SELECT a FROM <<{'a': FALSE}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.bool(true))),
                    Datum.struct(Field.of("a", Datum.bool(false)))
                )
            ),
            SuccessTestCase(
                name = "UNION INT and BIGINT",
                input = "SELECT a FROM <<{'a': 1}>> UNION SELECT a FROM <<{'a': CAST(2 AS BIGINT)}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.bigint(1))),
                    Datum.struct(Field.of("a", Datum.bigint(2)))
                )
            ),
            SuccessTestCase(
                name = "UNION multi-column same types",
                input = "SELECT a, b FROM <<{'a': 1, 'b': 'x'}>> UNION SELECT a, b FROM <<{'a': 2, 'b': 'y'}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.string("x"))),
                    Datum.struct(Field.of("a", Datum.integer(2)), Field.of("b", Datum.string("y")))
                )
            ),
            SuccessTestCase(
                name = "UNION SELECT VALUE integers",
                input = "SELECT VALUE a FROM <<{'a': 1}>> UNION SELECT VALUE a FROM <<{'a': 2}>>",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
            SuccessTestCase(
                name = "UNION INT and DECIMAL",
                input = "SELECT a FROM <<{'a': 1}>> UNION SELECT a FROM <<{'a': 1.0}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("1.0"), 11, 1))),
                )
            ),
            SuccessTestCase(
                name = "UNION DECIMAL and DECIMAL with different precision",
                input = "SELECT a FROM <<{'a': 12.234}>> UNION SELECT a FROM <<{'a': 1.23}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("12.234"), 5, 3))),
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("1.230"), 5, 3)))
                )
            ),
            SuccessTestCase(
                name = "UNION DISTINCT INT and DECIMAL",
                input = "SELECT DISTINCT a FROM << {'a': 1}, {'a': 1} >> UNION SELECT DISTINCT a FROM << {'a': 4.0} >>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("1.0"), 11, 1))),
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("4.0"), 11, 1)))
                )
            ),
        )

        @JvmStatic
        fun intersectSchemaCases() = listOf(
            SuccessTestCase(
                name = "INTERSECT INT and INT - matching",
                input = "SELECT VALUE t FROM <<1, 2, 3>> AS t INTERSECT SELECT VALUE t FROM <<2, 3, 4>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            SuccessTestCase(
                name = "INTERSECT INT and INT - no overlap",
                input = "SELECT VALUE t FROM <<1, 2>> AS t INTERSECT SELECT VALUE t FROM <<3, 4>> AS t",
                expected = Datum.bagVararg()
            ),
            SuccessTestCase(
                name = "INTERSECT ALL with duplicates",
                input = "SELECT VALUE t FROM <<1, 1, 2>> AS t INTERSECT ALL SELECT VALUE t FROM <<1, 1, 3>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(1)
                )
            ),
            SuccessTestCase(
                name = "INTERSECT rows - matching",
                input = "SELECT a FROM <<{'a': 1}, {'a': 2}>> INTERSECT SELECT a FROM <<{'a': 2}, {'a': 3}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(2)))
                )
            ),
            SuccessTestCase(
                name = "INTERSECT INT and BIGINT",
                input = "SELECT VALUE t FROM <<1, 2>> AS t INTERSECT SELECT VALUE t FROM <<CAST(2 AS BIGINT), CAST(3 AS BIGINT)>> AS t",
                expected = Datum.bagVararg(
                    Datum.bigint(2)
                )
            ),
        )

        @JvmStatic
        fun exceptSchemaCases() = listOf(
            SuccessTestCase(
                name = "EXCEPT INT and INT - remove matching",
                input = "SELECT VALUE t FROM <<1, 2, 3>> AS t EXCEPT SELECT VALUE t FROM <<2, 3, 4>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1)
                )
            ),
            SuccessTestCase(
                name = "EXCEPT INT and INT - no overlap",
                input = "SELECT VALUE t FROM <<1, 2>> AS t EXCEPT SELECT VALUE t FROM <<3, 4>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
            SuccessTestCase(
                name = "EXCEPT ALL with duplicates",
                input = "SELECT VALUE t FROM <<1, 1, 2, 2>> AS t EXCEPT ALL SELECT VALUE t FROM <<1, 2>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
            SuccessTestCase(
                name = "EXCEPT rows - remove matching",
                input = "SELECT a FROM <<{'a': 1}, {'a': 2}>> EXCEPT SELECT a FROM <<{'a': 2}, {'a': 3}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
            SuccessTestCase(
                name = "EXCEPT INT and BIGINT",
                input = "SELECT VALUE t FROM <<1, 2>> AS t EXCEPT SELECT VALUE t FROM <<CAST(2 AS BIGINT)>> AS t",
                expected = Datum.bagVararg(
                    Datum.bigint(1)
                )
            ),
            SuccessTestCase(
                name = "EXCEPT all matching",
                input = "SELECT VALUE t FROM <<1, 2>> AS t EXCEPT SELECT VALUE t FROM <<1, 2>> AS t",
                expected = Datum.bagVararg()
            ),
        )

        @JvmStatic
        fun unionNullMissingCases() = listOf(
            // NULL with NULL — distinct deduplicates
            SuccessTestCase(
                name = "UNION NULL and NULL - distinct",
                input = "SELECT VALUE t FROM <<NULL>> AS t UNION SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue()
                )
            ),
            // NULL with NULL — ALL preserves duplicates
            SuccessTestCase(
                name = "UNION ALL NULL and NULL",
                input = "SELECT VALUE t FROM <<NULL>> AS t UNION ALL SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue(),
                    Datum.nullValue()
                )
            ),
            // MISSING with MISSING — distinct deduplicates
            SuccessTestCase(
                name = "UNION MISSING and MISSING - distinct",
                input = "SELECT VALUE t FROM <<MISSING>> AS t UNION SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.missing()
                )
            ),
            // MISSING with MISSING — ALL preserves duplicates
            SuccessTestCase(
                name = "UNION ALL MISSING and MISSING",
                input = "SELECT VALUE t FROM <<MISSING>> AS t UNION ALL SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue(),
                    Datum.nullValue()
                )
            ),
            // NULL and MISSING are distinct values
            SuccessTestCase(
                name = "UNION NULL and MISSING - distinct",
                input = "SELECT VALUE t FROM <<NULL>> AS t UNION SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue(),
                )
            ),
            // INT with NULL
            SuccessTestCase(
                name = "UNION INT and NULL",
                input = "SELECT VALUE t FROM <<1, 2>> AS t UNION SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.nullValue()
                )
            ),
            // INT with MISSING
            SuccessTestCase(
                name = "UNION INT and MISSING",
                input = "SELECT VALUE t FROM <<1, 2>> AS t UNION SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.missing()
                )
            ),
            // Mixed: INT, NULL, MISSING
            SuccessTestCase(
                name = "UNION ALL INT, NULL, and MISSING",
                input = "SELECT VALUE t FROM <<1, NULL>> AS t UNION ALL SELECT VALUE t FROM <<MISSING, 1>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.nullValue(),
                    Datum.nullValue(),
                    Datum.integer(1)
                )
            ),
            // Struct rows with NULL column values
            SuccessTestCase(
                name = "UNION rows with NULL column values - distinct",
                input = "SELECT a FROM <<{'a': NULL}>> UNION SELECT a FROM <<{'a': NULL}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.nullValue()))
                )
            ),
            // Struct rows with NULL and non-NULL
            SuccessTestCase(
                name = "UNION rows with NULL and INT column values",
                input = "SELECT a FROM <<{'a': NULL}>> UNION SELECT a FROM <<{'a': 1}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.nullValue())),
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
        )

        // =====================
        // INTERSECT NULL/MISSING cases
        // =====================

        @JvmStatic
        fun intersectNullMissingCases() = listOf(
            // NULL intersect NULL
            SuccessTestCase(
                name = "INTERSECT NULL and NULL",
                input = "SELECT VALUE t FROM <<NULL>> AS t INTERSECT SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue()
                )
            ),
            // MISSING intersect MISSING
            SuccessTestCase(
                name = "INTERSECT MISSING and MISSING",
                input = "SELECT VALUE t FROM <<MISSING>> AS t INTERSECT SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue()
                )
            ),
            // NULL and MISSING — no overlap
            SuccessTestCase(
                name = "INTERSECT NULL and MISSING - no overlap",
                input = "SELECT VALUE t FROM <<NULL>> AS t INTERSECT SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue()
                )
            ),
            // INT and NULL — no overlap
            SuccessTestCase(
                name = "INTERSECT INT and NULL - no overlap",
                input = "SELECT VALUE t FROM <<1, 2>> AS t INTERSECT SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg()
            ),
            // INT and MISSING — no overlap
            SuccessTestCase(
                name = "INTERSECT INT and MISSING - no overlap",
                input = "SELECT VALUE t FROM <<1, 2>> AS t INTERSECT SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg()
            ),
            // Mixed with common NULL
            SuccessTestCase(
                name = "INTERSECT mixed with common NULL",
                input = "SELECT VALUE t FROM <<1, NULL, 2>> AS t INTERSECT SELECT VALUE t FROM <<NULL, 3>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue()
                )
            ),
            // INTERSECT ALL with duplicate NULLs
            SuccessTestCase(
                name = "INTERSECT ALL with duplicate NULLs",
                input = "SELECT VALUE t FROM <<NULL, NULL, 1>> AS t INTERSECT ALL SELECT VALUE t FROM <<NULL, NULL, 2>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue(),
                    Datum.nullValue()
                )
            ),
            // Struct rows with NULL column — matching
            SuccessTestCase(
                name = "INTERSECT rows with NULL column values - matching",
                input = "SELECT a FROM <<{'a': NULL}, {'a': 1}>> INTERSECT SELECT a FROM <<{'a': NULL}, {'a': 2}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.nullValue()))
                )
            ),
        )

        // =====================
        // EXCEPT NULL/MISSING cases
        // =====================

        @JvmStatic
        fun exceptNullMissingCases() = listOf(
            // NULL except NULL — removed
            SuccessTestCase(
                name = "EXCEPT NULL and NULL - removed",
                input = "SELECT VALUE t FROM <<NULL>> AS t EXCEPT SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg()
            ),
            // MISSING except MISSING — removed
            SuccessTestCase(
                name = "EXCEPT MISSING and MISSING - removed",
                input = "SELECT VALUE t FROM <<MISSING>> AS t EXCEPT SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg()
            ),
            // NULL except MISSING — NULL kept
            SuccessTestCase(
                name = "EXCEPT NULL and MISSING - NULL kept",
                input = "SELECT VALUE t FROM <<NULL>> AS t EXCEPT SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg()
            ),
            // MISSING except NULL — MISSING kept
            SuccessTestCase(
                name = "EXCEPT MISSING and NULL - MISSING kept",
                input = "SELECT VALUE t FROM <<MISSING>> AS t EXCEPT SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg()
            ),
            // INT, NULL except NULL — INT kept
            SuccessTestCase(
                name = "EXCEPT INT and NULL from NULL - INT kept",
                input = "SELECT VALUE t FROM <<1, NULL>> AS t EXCEPT SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1)
                )
            ),
            // INT, MISSING except MISSING — INT kept
            SuccessTestCase(
                name = "EXCEPT INT and MISSING from MISSING - INT kept",
                input = "SELECT VALUE t FROM <<1, MISSING>> AS t EXCEPT SELECT VALUE t FROM <<MISSING>> AS t",
                expected = Datum.bagVararg(
                    Datum.integer(1)
                )
            ),
            // EXCEPT ALL with duplicate NULLs
            SuccessTestCase(
                name = "EXCEPT ALL with duplicate NULLs",
                input = "SELECT VALUE t FROM <<NULL, NULL, 1>> AS t EXCEPT ALL SELECT VALUE t FROM <<NULL>> AS t",
                expected = Datum.bagVararg(
                    Datum.nullValue(),
                    Datum.integer(1)
                )
            ),
            // Struct rows — NULL column removed
            SuccessTestCase(
                name = "EXCEPT rows with NULL column values - removed",
                input = "SELECT a FROM <<{'a': NULL}, {'a': 1}>> EXCEPT SELECT a FROM <<{'a': NULL}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
        )

        // =====================
        // OUTER UNION cases
        // =====================

        @JvmStatic
        fun outerUnionCases() = listOf(
            // Incompatible types succeed with OUTER
            SuccessTestCase(
                name = "OUTER UNION INT and STRING",
                input = "SELECT a FROM <<{'a': 1}>> OUTER UNION SELECT a FROM <<{'a': 'x'}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.string("x")))
                )
            ),
            SuccessTestCase(
                name = "OUTER UNION INT and BOOL",
                input = "SELECT a FROM <<{'a': 1}>> OUTER UNION SELECT a FROM <<{'a': TRUE}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.bool(true)))
                )
            ),
            SuccessTestCase(
                name = "OUTER UNION column count mismatch",
                input = "SELECT a FROM <<{'a': 1}>> OUTER UNION SELECT a, b FROM <<{'a': 2, 'b': 3}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.integer(2)), Field.of("b", Datum.integer(3)))
                )
            ),
            // OUTER UNION ALL with incompatible types
            SuccessTestCase(
                name = "OUTER UNION ALL INT and STRING",
                input = "SELECT a FROM <<{'a': 1}>> OUTER UNION ALL SELECT a FROM <<{'a': 'x'}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1))),
                    Datum.struct(Field.of("a", Datum.string("x")))
                )
            ),
            SuccessTestCase(
                name = "Outer UNION INT and DECIMAL",
                input = "SELECT a FROM <<{'a': 1}>> OUTER UNION SELECT a FROM <<{'a': 1.0}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
            SuccessTestCase(
                name = "Outer UNION DECIMAL and DECIMAL with different precision",
                input = "SELECT a FROM <<{'a': 12.234}>> OUTER UNION SELECT a FROM <<{'a': 1.23}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("12.234"), 5, 3))),
                    Datum.struct(Field.of("a", Datum.decimal(java.math.BigDecimal("1.23"), 3, 2)))
                )
            ),
        )

        // =====================
        // OUTER INTERSECT cases
        // =====================

        @JvmStatic
        fun outerIntersectCases() = listOf(
            // Incompatible types — no common values, empty result
            SuccessTestCase(
                name = "OUTER INTERSECT INT and STRING - no overlap",
                input = "SELECT a FROM <<{'a': 1}>> OUTER INTERSECT SELECT a FROM <<{'a': 'x'}>>",
                expected = Datum.bagVararg()
            ),
            // Column count mismatch — no common values, empty result
            SuccessTestCase(
                name = "OUTER INTERSECT column count mismatch",
                input = "SELECT a FROM <<{'a': 1}>> OUTER INTERSECT SELECT a, b FROM <<{'a': 1, 'b': 2}>>",
                expected = Datum.bagVararg()
            ),
        )

        // =====================
        // OUTER EXCEPT cases
        // =====================

        @JvmStatic
        fun outerExceptCases() = listOf(
            // Incompatible types — nothing removed, keep LHS
            SuccessTestCase(
                name = "OUTER EXCEPT INT and STRING - keep all LHS",
                input = "SELECT a FROM <<{'a': 1}>> OUTER EXCEPT SELECT a FROM <<{'a': 'x'}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
            // Column count mismatch — nothing removed, keep LHS
            SuccessTestCase(
                name = "OUTER EXCEPT column count mismatch",
                input = "SELECT a FROM <<{'a': 1}>> OUTER EXCEPT SELECT a, b FROM <<{'a': 1, 'b': 2}>>",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("a", Datum.integer(1)))
                )
            ),
        )
    }
}

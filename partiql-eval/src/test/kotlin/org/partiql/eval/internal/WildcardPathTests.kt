package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.value.Datum
import java.math.BigDecimal

/**
 * Comprehensive tests for wildcard path expressions ([*] and .*).
 *
 * Tests cover:
 * 1. [*] on literal arrays
 * 2. .* on literal structs
 * 3. [*] and .* on variables in SELECT clauses
 * 4. [*] and .* on wrapped SFW (SELECT-FROM-WHERE) expressions — the original bug
 * 5. Chained wildcards
 * 6. Wildcards with global table references
 * 7. Edge/failure cases
 */
class WildcardPathTests {

    @ParameterizedTest
    @MethodSource("allElementsOnLiteralArrayCases")
    fun allElementsOnLiteralArray(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("allFieldsOnLiteralStructCases")
    fun allFieldsOnLiteralStruct(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("wildcardOnVariableInSelectCases")
    fun wildcardOnVariableInSelect(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("allElementsOnWrappedSfwCases")
    fun allElementsOnWrappedSfw(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("allFieldsOnWrappedSfwCases")
    fun allFieldsOnWrappedSfw(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("chainedWildcardCases")
    fun chainedWildcards(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("wildcardWithGlobalsCases")
    fun wildcardWithGlobals(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("wildcardEdgeCases")
    fun wildcardEdgeCases(tc: SuccessTestCase) = tc.run()

    companion object {

        // =====================================================================
        // 1. [*] on literal arrays
        // =====================================================================
        @JvmStatic
        fun allElementsOnLiteralArrayCases() = listOf(
            // Simple: [1, 2, 3][*]
            SuccessTestCase(
                name = "[*] on literal integer array",
                input = "[1, 2, 3][*]",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            // [*] followed by field access: [{'a':1}, {'a':2}][*].a
            SuccessTestCase(
                name = "[*] on literal array of structs with field access",
                input = "[{'a': 1}, {'a': 2}, {'a': 3}][*].a",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            // Nested [*]: [[1,2],[3,4]][*][*]
            SuccessTestCase(
                name = "[*][*] on nested literal arrays",
                input = "[[1, 2], [3, 4]][*][*]",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                    Datum.integer(4)
                )
            ),
            // Empty array: [][*]
            SuccessTestCase(
                name = "[*] on empty literal array",
                input = "[][*]",
                expected = Datum.bag(emptyList())
            ),
        )

        // =====================================================================
        // 2. .* on literal structs
        // =====================================================================
        @JvmStatic
        fun allFieldsOnLiteralStructCases() = listOf(
            // Simple: {'a':1, 'b':2}.*
            SuccessTestCase(
                name = ".* on literal struct",
                input = "{'a': 1, 'b': 2}.*",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
            // .* with nested field access: [{'a':{'x':1,'y':2}}][*].a.*
            SuccessTestCase(
                name = "[*].field.* on literal array of nested structs",
                input = "[{'a': {'x': 1, 'y': 2}}, {'a': {'x': 3, 'y': 4}}][*].a.*",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                    Datum.integer(4)
                )
            ),
        )

        // =====================================================================
        // 3. [*] and .* on variables in SELECT clause
        // =====================================================================
        @JvmStatic
        fun wildcardOnVariableInSelectCases() = listOf(
            // SELECT VALUE t.arr[*] FROM ...
            SuccessTestCase(
                name = "[*] on variable field in SELECT VALUE",
                input = """
                    SELECT VALUE v
                    FROM <<{'arr': [1, 2, 3]}>> AS t, t.arr[*] AS v
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            // SELECT t.arr[*].name FROM ...
            SuccessTestCase(
                name = "[*].field on variable in SELECT",
                input = """
                    SELECT VALUE v.name
                    FROM <<{'arr': [{'name': 'Alice'}, {'name': 'Bob'}]}>> AS t, t.arr[*] AS v
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.string("Alice"),
                    Datum.string("Bob")
                )
            ),
            // SELECT VALUE t.s.* FROM ... using .* on variable
            SuccessTestCase(
                name = ".* on variable field in SELECT VALUE via unpivot join",
                input = """
                    SELECT VALUE v
                    FROM <<{'s': {'x': 10, 'y': 20}}>> AS t, t.s.* AS v
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(10),
                    Datum.integer(20)
                )
            ),
        )

        // =====================================================================
        // 4. [*] on wrapped SFW expressions (the original bug)
        // =====================================================================
        @JvmStatic
        fun allElementsOnWrappedSfwCases() = listOf(
            // Core fix: (SELECT VALUE t FROM <<1,2,3>> AS t)[*]
            SuccessTestCase(
                name = "[*] on SFW returning bag of scalars",
                input = "(SELECT VALUE t FROM <<1, 2, 3>> AS t)[*]",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3)
                )
            ),
            // (SELECT t.a FROM <<{'a':1},{'a':2}>> AS t)[*].a
            SuccessTestCase(
                name = "[*].field on SFW returning bag of structs",
                input = "(SELECT t.a FROM <<{'a': 1}, {'a': 2}>> AS t)[*].a",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
            // Original query pattern: (SELECT t.name FROM ... AS t)[*].name
            SuccessTestCase(
                name = "[*].field on SFW — original bug pattern",
                input = "(SELECT t.name FROM <<{'name': 'Alice'}, {'name': 'Bob'}>> AS t)[*].name",
                expected = Datum.bagVararg(
                    Datum.string("Alice"),
                    Datum.string("Bob")
                )
            ),
            // SELECT VALUE on SFW with [*]
            SuccessTestCase(
                name = "[*] on SELECT VALUE SFW",
                input = "(SELECT VALUE t.a FROM <<{'a': 10}, {'a': 20}>> AS t)[*]",
                expected = Datum.bagVararg(
                    Datum.integer(10),
                    Datum.integer(20)
                )
            ),
        )

        // =====================================================================
        // 5. .* on wrapped SFW expressions
        // =====================================================================
        @JvmStatic
        fun allFieldsOnWrappedSfwCases() = listOf(
            // (SELECT VALUE {'x':1,'y':2} FROM <<1>> AS t)[*].*
            SuccessTestCase(
                name = "[*].* on SFW returning bag of structs",
                input = "(SELECT VALUE {'x': 1, 'y': 2} FROM <<1>> AS t)[*].*",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
        )

        // =====================================================================
        // 6. Chained wildcards
        // =====================================================================
        @JvmStatic
        fun chainedWildcardCases() = listOf(
            // (SELECT VALUE [{'a':1},{'a':2}] FROM <<1>> AS t)[*][*].a
            SuccessTestCase(
                name = "[*][*].field on SFW returning bag of arrays of structs",
                input = "(SELECT VALUE [{'a': 1}, {'a': 2}] FROM <<1>> AS t)[*][*].a",
                expected = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.integer(2)
                )
            ),
            // Multiple levels: array[*].struct.array[*].field
            SuccessTestCase(
                name = "multi-level [*] with nested arrays and structs",
                input = """
                    [
                        {'items': [{'val': 10}, {'val': 20}]},
                        {'items': [{'val': 30}]}
                    ][*].items[*].val
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(10),
                    Datum.integer(20),
                    Datum.integer(30)
                )
            ),
        )

        // =====================================================================
        // 7. Wildcards with global table references
        // =====================================================================
        @JvmStatic
        fun wildcardWithGlobalsCases() = listOf(
            // Original bug: (SELECT t.feature_set_name FROM table AS t)[*].feature_set_name
            SuccessTestCase(
                name = "[*].field on SFW over global table — original reported bug",
                input = "(SELECT t.feature_set_name FROM my_table AS t)[*].feature_set_name",
                expected = Datum.bagVararg(
                    Datum.string("feature_a"),
                    Datum.string("feature_b")
                ),
                globals = listOf(
                    Global(
                        name = "my_table",
                        value = """
                            [
                                { "feature_set_name": "feature_a" },
                                { "feature_set_name": "feature_b" }
                            ]
                        """
                    )
                )
            ),
            // [*] on global variable directly
            SuccessTestCase(
                name = "[*] on global array table",
                input = "my_array[*]",
                expected = Datum.bagVararg(
                    Datum.bigint(1),
                    Datum.bigint(2),
                    Datum.bigint(3)
                ),
                globals = listOf(
                    Global(
                        name = "my_array",
                        value = "[1, 2, 3]"
                    )
                )
            ),
            // [*].field on global array of structs
            SuccessTestCase(
                name = "[*].field on global array of structs",
                input = "my_data[*].name",
                expected = Datum.bagVararg(
                    Datum.string("Alice"),
                    Datum.string("Bob")
                ),
                globals = listOf(
                    Global(
                        name = "my_data",
                        value = """
                            [
                                { "name": "Alice" },
                                { "name": "Bob" }
                            ]
                        """
                    )
                )
            ),
        )

        // =====================================================================
        // 8. Edge cases
        // =====================================================================
        @JvmStatic
        fun wildcardEdgeCases() = listOf(
            // [*] on a single-element array
            SuccessTestCase(
                name = "[*] on single-element array",
                input = "[42][*]",
                expected = Datum.bagVararg(
                    Datum.integer(42)
                )
            ),
            // .* on a single-field struct
            SuccessTestCase(
                name = ".* on single-field struct",
                input = "{'only': 99}.*",
                expected = Datum.bagVararg(
                    Datum.integer(99)
                )
            ),
            // [*] on scalar in permissive mode should yield bag with the scalar (coercion)
            SuccessTestCase(
                name = "[*] on scalar in permissive mode",
                input = "1[*]",
                expected = Datum.bagVararg(
                    Datum.integer(1)
                ),
                mode = Mode.PERMISSIVE()
            ),
            // .* on scalar in permissive mode
            SuccessTestCase(
                name = ".* on scalar in permissive mode",
                input = "1.0.*",
                expected = Datum.bagVararg(Datum.decimal(BigDecimal.ONE, 2, 1)),
                mode = Mode.PERMISSIVE()
            ),
            // [*] on NULL
            SuccessTestCase(
                name = "[*] on NULL in permissive mode",
                input = "NULL[*]",
                expected = Datum.bagVararg(
                    Datum.nullValue()
                ),
                mode = Mode.PERMISSIVE()
            ),
            // .* on NULL
            SuccessTestCase(
                name = ".* on NULL in permissive mode",
                input = "NULL.*",
                expected = Datum.bagVararg(Datum.nullValue()),
                mode = Mode.PERMISSIVE()
            ),
        )
    }

    // =====================================================================
    // Strict mode failure cases
    // =====================================================================
    @Test
    fun allElementsOnScalarStrictModeThrows() {
        FailureTestCase(
            name = "[*] on scalar in strict mode should error",
            input = "1[*]",
            mode = Mode.STRICT()
        ).run()
    }

    @Test
    fun allFieldsOnScalarStrictModeThrows() {
        FailureTestCase(
            name = ".* on scalar in strict mode should error",
            input = "1.0.*",
            mode = Mode.STRICT()
        ).run()
    }
}

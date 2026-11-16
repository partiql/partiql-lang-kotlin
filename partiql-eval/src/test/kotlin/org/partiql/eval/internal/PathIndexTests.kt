/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.Mode
import org.partiql.eval.PTestCase
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import kotlin.test.assertEquals

/**
 * Path index (`a[i]`) tests.
 *
 * - Errors in ORDER BY and window keys become MISSING in permissive mode (#1982).
 * - Indexing a VARIANT (Ion list or S-expression) works like indexing an ARRAY (#1983).
 * - A null array gives NULL, and an index is never narrowed to INT.
 */
class PathIndexTests {

    @ParameterizedTest
    @MethodSource("permissiveSortKeyCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun permissiveSortKey(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("strictSortKeyCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun strictSortKey(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("nullArrayIndexErrorCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun nullArrayIndexError(tc: PTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("indexSuccessCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun indexSuccess(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("indexFailurePermissiveCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun indexFailurePermissive(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("indexFailureStrictCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun indexFailureStrict(tc: FailureTestCase) = tc.run()

    /**
     * Sort and window keys are now wrapped for permissive mode. The wrapper must still rethrow errors that are not
     * data errors, such as an interrupt, so a query cannot keep running after it is cancelled.
     */
    @Test
    fun permissiveWrapperRethrowsInterrupt() {
        val interrupted = PError(PError.INTERRUPTED, Severity.ERROR(), PErrorKind.EXECUTION(), null, emptyMap())
        val throwing = object : ExprValue {
            override fun eval(env: Environment): Datum = throw PRuntimeException(interrupted)
        }
        val e = assertThrows<PRuntimeException> { ExprPermissive(throwing).eval(Environment()) }
        assertEquals(PError.INTERRUPTED, e.error.code())
    }

    companion object {

        private val PERMISSIVE = Mode.PERMISSIVE()
        private val STRICT = Mode.STRICT()

        private val variantGlobals = listOf(
            Global("v", Datum.ion("[1, 2]"), PType.variant("ion")),
            Global("vs", Datum.ion("{a: [1, 2], i: 1}"), PType.variant("ion")),
            Global("vn", Datum.ion("null.list"), PType.variant("ion")),
        )

        private fun ints(vararg values: Int): Datum = Datum.array(values.map { Datum.integer(it) })

        @JvmStatic
        fun permissiveSortKeyCases() = listOf(
            SuccessTestCase(
                name = "ORDER BY out-of-range index",
                input = "SELECT VALUE x FROM <<1, 2>> AS x ORDER BY [1, 2][5], x",
                expected = ints(1, 2),
                mode = PERMISSIVE,
            ),
            SuccessTestCase(
                name = "ORDER BY missing key (regression)",
                input = "SELECT VALUE x FROM <<2, 1>> AS x ORDER BY {'a': 1}.b, x",
                expected = ints(1, 2),
                mode = PERMISSIVE,
            ),
            SuccessTestCase(
                name = "ORDER BY failed CAST",
                input = "SELECT VALUE x FROM <<2, 1>> AS x ORDER BY CAST('a' AS INT), x",
                expected = ints(1, 2),
                mode = PERMISSIVE,
            ),
            SuccessTestCase(
                name = "ORDER BY failed CAST inside CASE",
                input = "SELECT VALUE x FROM <<2, 1>> AS x ORDER BY CASE WHEN true THEN CAST('a' AS INT) END, x",
                expected = ints(1, 2),
                mode = PERMISSIVE,
            ),
            SuccessTestCase(
                name = "ORDER BY out-of-range index on a row value",
                input = "SELECT VALUE t.x FROM <<{'x': 2, 'a': [1]}, {'x': 1, 'a': [1]}>> AS t ORDER BY t.a[5], t.x",
                expected = ints(1, 2),
                mode = PERMISSIVE,
            ),
            SuccessTestCase(
                name = "window ORDER BY out-of-range index",
                input = "SELECT x, ROW_NUMBER() OVER (ORDER BY [1, 2][5], x) AS r FROM <<2, 1>> AS x",
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("x", Datum.integer(1)), Field.of("r", Datum.bigint(1))),
                    Datum.struct(Field.of("x", Datum.integer(2)), Field.of("r", Datum.bigint(2))),
                ),
                mode = PERMISSIVE,
            ),
        )

        @JvmStatic
        fun strictSortKeyCases() = listOf(
            FailureTestCase(
                name = "ORDER BY out-of-range index",
                input = "SELECT VALUE x FROM <<1, 2>> AS x ORDER BY [1, 2][5]",
                expectedCode = PError.PATH_INDEX_NEVER_SUCCEEDS,
            ),
            FailureTestCase(
                name = "ORDER BY failed CAST",
                input = "SELECT VALUE x FROM <<1, 2>> AS x ORDER BY CAST('a' AS INT)",
                expectedCode = PError.INVALID_CHAR_VALUE_FOR_CAST,
            ),
            FailureTestCase(
                name = "window ORDER BY out-of-range index",
                input = "SELECT ROW_NUMBER() OVER (ORDER BY [1, 2][5]) AS r FROM <<1, 2>> AS x",
                expectedCode = PError.PATH_INDEX_NEVER_SUCCEEDS,
            ),
        )

        /**
         * The index is evaluated before the null check, so an error in it is not hidden by a null array.
         */
        @JvmStatic
        fun nullArrayIndexErrorCases() = listOf(
            SuccessTestCase(
                name = "null array with a failing index",
                input = "CAST(NULL AS LIST)[CAST('a' AS INT)]",
                expected = Datum.missing(),
                mode = PERMISSIVE,
            ),
            FailureTestCase(
                name = "null array with a failing index",
                input = "CAST(NULL AS LIST)[CAST('a' AS INT)]",
                expectedCode = PError.INVALID_CHAR_VALUE_FOR_CAST,
            ),
        )

        /**
         * These give the same result in both modes.
         */
        @JvmStatic
        fun indexSuccessCases() = listOf(
            "`[1, 2]`[0]" to Datum.integer(1),
            "`[1, 2]`[1]" to Datum.integer(2),
            "`{a: [1, 2]}`.a[1]" to Datum.integer(2),
            "`[[1, 2], [3, 4]]`[1][0]" to Datum.integer(3),
            "`[{a: 1}, {a: 2}]`[1].a" to Datum.integer(2),
            "`(1 2)`[1]" to Datum.integer(2),
            "`[1, 2]`[`0`]" to Datum.integer(1),
            "`[1, 2]`[CAST(1 AS BIGINT)]" to Datum.integer(2),
            "[10, 20][`1`]" to Datum.integer(20),
            "[10, 20][CAST(1 AS TINYINT)]" to Datum.integer(20),
            "[10, 20][CAST(1 AS SMALLINT)]" to Datum.integer(20),
            "[10, 20][CAST(1 AS BIGINT)]" to Datum.integer(20),
            "v[0]" to Datum.integer(1),
            "vs.a[1]" to Datum.integer(2),
            "vs.a[vs.i]" to Datum.integer(2),
            "`null.list`[0]" to Datum.nullValue(),
            "`null.sexp`[0]" to Datum.nullValue(),
            "vn[0]" to Datum.nullValue(),
            "CAST(NULL AS LIST)[0]" to Datum.nullValue(),
            "SELECT VALUE t[0] FROM <<`[1]`, `[2, 3]`>> AS t" to Datum.bagVararg(Datum.integer(1), Datum.integer(2)),
        ).flatMap { (query, expected) ->
            listOf(PERMISSIVE, STRICT).map { mode ->
                SuccessTestCase(name = query, input = query, expected = expected, mode = mode, globals = variantGlobals)
            }
        }

        /**
         * Each of these is MISSING in permissive mode and fails with PATH_INDEX_NEVER_SUCCEEDS in strict mode.
         */
        private val failingIndexes = listOf(
            "`[1, 2]`[2]",
            "`[1, 2]`[-1]",
            "`[1, 2]`[`1.5`]",
            "`[1, 2]`[`\"0\"`]",
            "`[1, 2]`[`4294967296`]",
            "`[1, 2]`[NULL]", // TODO(#1991): SQL:2023 section 6.25 gives NULL for a NULL index
            "`[]`[0]",
            "`null`[0]",
            "`null.struct`[0]",
            "`{a: 1}`[0]",
            "`1`[0]",
            "`\"ab\"`[0]",
            "v[2]",
            "[10, 20][4294967296]",
            "[10, 20][4294967297]",
            "[10, 20][-1]",
            "[10, 20][-4294967296]",
            "[10, 20][CAST(NULL AS INT)]", // TODO(#1991): SQL:2023 section 6.25 gives NULL for a NULL index
        )

        @JvmStatic
        fun indexFailurePermissiveCases() = failingIndexes.map { query ->
            SuccessTestCase(name = query, input = query, expected = Datum.missing(), mode = PERMISSIVE, globals = variantGlobals)
        }

        @JvmStatic
        fun indexFailureStrictCases() = failingIndexes.map { query ->
            FailureTestCase(name = query, input = query, globals = variantGlobals, expectedCode = PError.PATH_INDEX_NEVER_SUCCEEDS)
        }
    }
}

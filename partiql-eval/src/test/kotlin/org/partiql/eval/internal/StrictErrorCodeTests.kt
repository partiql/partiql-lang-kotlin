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
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rex.RexError
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.value.Datum
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * In strict mode, an expression that the planner proves will fail raises the planner's error code at runtime, not
 * INTERNAL_ERROR (#1988). Permissive mode is unchanged: the expression is MISSING.
 */
class StrictErrorCodeTests {

    @ParameterizedTest
    @MethodSource("strictCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun strict(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("permissiveCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun permissive(tc: SuccessTestCase) = tc.run()

    /**
     * The runtime error is a runtime error: severity ERROR and kind EXECUTION, like the errors raised by checks that only
     * happen at runtime, even though the planner reported it as a SEMANTIC warning.
     */
    @Test
    fun runtimeErrorIsExecutionError() {
        val e = assertThrows<PRuntimeException> { executeStrict("SELECT t.p.id AS id FROM <<{'p': 'x'}>> AS t") }
        assertEquals(PError.PATH_SYMBOL_NEVER_SUCCEEDS, e.error.code())
        assertEquals(Severity.ERROR(), e.error.severity)
        assertEquals(PErrorKind.EXECUTION(), e.error.kind)
    }

    /**
     * A compiled plan can be cached and run many times, possibly on several threads. Changing the error from one run
     * must not change the error the next run raises.
     */
    @Test
    fun eachRunRaisesANewError() {
        val prepared = prepareStrict("1[0]")
        val first = assertThrows<PRuntimeException> { DatumMaterialize.materialize(prepared.execute()) }
        first.error.severity = Severity.WARNING()
        first.error.kind = PErrorKind.SEMANTIC()
        val second = assertThrows<PRuntimeException> { DatumMaterialize.materialize(prepared.execute()) }
        assertNotSame(first.error, second.error)
        assertEquals(PError.PATH_INDEX_NEVER_SUCCEEDS, second.error.code())
        assertEquals(Severity.ERROR(), second.error.severity)
        assertEquals(PErrorKind.EXECUTION(), second.error.kind)
    }

    /**
     * [RexError.getError] returns a copy, so a caller cannot change a plan that may be shared.
     */
    @Test
    fun rexErrorKeepsACopy() {
        val reported = PError(PError.PATH_KEY_NEVER_SUCCEEDS, Severity.WARNING(), PErrorKind.SEMANTIC(), null, null)
        val rex = RexError.create(reported)
        reported.severity = Severity.ERROR()
        val first = rex.error!!
        first.kind = PErrorKind.EXECUTION()
        val second = rex.error!!
        assertEquals(PError.PATH_KEY_NEVER_SUCCEEDS, second.code())
        assertEquals(Severity.WARNING(), second.severity)
        assertEquals(PErrorKind.SEMANTIC(), second.kind)
        assertEquals(null, RexError.create().error)
    }

    /**
     * A non-numeric OFFSET used to become a second LIMIT node in the plan.
     */
    @Test
    fun invalidOffsetIsAnOffsetNode() {
        val plan = planner.plan(parser.parse("SELECT VALUE x FROM <<1, 2, 3>> AS x LIMIT 2 OFFSET 'a'").statements[0], session).plan
        val operators = walk((plan.action as Action.Query).rex).toList()
        assertTrue(operators.any { it is RelOffset }, "Expected an OFFSET node in: $operators")
        assertEquals(1, operators.count { it is RelLimit }, "Expected one LIMIT node in: $operators")
    }

    companion object {

        private val parser = PartiQLParser.standard()
        private val planner = PartiQLPlanner.standard()
        private val compiler = PartiQLCompiler.standard()
        private val session = Session.builder().catalog("memory").catalogs(Catalog.builder().name("memory").build()).build()

        private fun prepareStrict(query: String) =
            compiler.prepare(planner.plan(parser.parse(query).statements[0], session).plan, Mode.STRICT())

        private fun executeStrict(query: String): Datum = DatumMaterialize.materialize(prepareStrict(query).execute())

        private fun walk(op: Operator): Sequence<Operator> =
            sequenceOf(op) + op.operands.asSequence().flatMap { operand -> operand.asSequence().flatMap { walk(it) } }

        /**
         * Each query is rejected by the planner with a warning, and the strict runtime error has the warning's code.
         */
        private val cases = listOf(
            "SELECT t.p.id AS id FROM <<{'p': 'x'}>> AS t" to PError.PATH_SYMBOL_NEVER_SUCCEEDS,
            "'not a tuple'.a" to PError.PATH_SYMBOL_NEVER_SUCCEEDS,
            "(NULL).a" to PError.PATH_SYMBOL_NEVER_SUCCEEDS,
            // #1993 tracks reporting PATH_SYMBOL_NEVER_SUCCEEDS for a missing field on a known struct.
            "{'a': 1}.b" to PError.VAR_REF_NOT_FOUND,
            "SELECT t.b AS b FROM <<{'a': 1}>> AS t" to PError.VAR_REF_NOT_FOUND,
            "{'a': 1}['b']" to PError.PATH_KEY_NEVER_SUCCEEDS,
            "[1, 2]['a']" to PError.PATH_KEY_NEVER_SUCCEEDS,
            "1[0]" to PError.PATH_INDEX_NEVER_SUCCEEDS,
            "(NULL)[0]" to PError.PATH_INDEX_NEVER_SUCCEEDS,
            "(MISSING)[0]" to PError.PATH_INDEX_NEVER_SUCCEEDS,
            "{'a': 1}[0]" to PError.PATH_INDEX_NEVER_SUCCEEDS,
            "[1, 2][1.0]" to PError.PATH_INDEX_NEVER_SUCCEEDS,
            "UPPER(1)" to PError.FUNCTION_TYPE_MISMATCH,
            "SELECT VALUE x FROM <<1, 2>> AS x LIMIT 'a'" to PError.TYPE_UNEXPECTED,
            "SELECT VALUE x FROM <<1, 2, 3>> AS x OFFSET 'a'" to PError.TYPE_UNEXPECTED,
            "SELECT VALUE x FROM <<1, 2, 3>> AS x LIMIT 2 OFFSET 'a'" to PError.TYPE_UNEXPECTED,
        )

        @JvmStatic
        fun strictCases() = cases.map { (query, code) ->
            FailureTestCase(name = query, input = query, expectedCode = code)
        }

        @JvmStatic
        fun permissiveCases() = listOf(
            "'not a tuple'.a",
            "{'a': 1}['b']",
            "1[0]",
            "UPPER(1)",
        ).map { query ->
            SuccessTestCase(name = query, input = query, expected = Datum.missing(), mode = Mode.PERMISSIVE())
        }
    }
}

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

package org.partiql.planner.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.util.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the inlining of WITH list elements (the default, see `PartiQLPlannerBuilder.forceInlineWithClause`).
 *
 * An inlined element is re-typed where it is referenced. A resolved variable of an enclosing query (an outer reference),
 * or a lateral (correlated) join, of the element would then refer to the wrong scope; this is reported as unsupported.
 */
class WithInliningTest {

    private val parser = PartiQLParser.standard()

    private val session = Session.builder()
        .catalog("default")
        .catalogs(Catalog.builder().name("default").build())
        .build()

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupported")
    fun `unsupported inlined WITH list elements are reported`(name: String, query: String, feature: String) {
        val errors = plan(query, PartiQLPlanner.standard())
        val features = errors.errors
            .filter { it.code() == PError.FEATURE_NOT_SUPPORTED }
            .map { it.getOrNull("FEATURE_NAME", String::class.java)!! }
        assertEquals(listOf(feature), features, "Received: ${errors.problems}")
        assertTrue(errors.errors.none { it.code() == PError.INTERNAL_ERROR }, "Received: ${errors.problems}")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supported")
    fun `supported inlined WITH list elements are planned`(name: String, query: String) {
        val errors = plan(query, PartiQLPlanner.standard())
        assertTrue(errors.problems.isEmpty(), "Unexpected problems: ${errors.problems}")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedQueries")
    fun `WITH list elements that are not inlined are planned`(query: String) {
        val planner = PartiQLPlanner.builder().forceInlineWithClause(false).build()
        val errors = plan(query, planner)
        assertTrue(errors.errors.isEmpty(), "Unexpected problems: ${errors.problems}")
    }

    private fun plan(query: String, planner: PartiQLPlanner): PErrorCollector {
        val statement = parser.parse(query).statements.single()
        val errors = PErrorCollector()
        planner.plan(statement, session, Context.of(errors))
        return errors
    }

    companion object {

        private const val OUTER_REFERENCE = "WITH list element referencing a variable of an enclosing query"
        private const val LATERAL = "WITH list element containing a lateral (correlated) join"

        private val unsupportedCases = listOf(
            Arguments.of(
                "reference to a variable of an enclosing query",
                "SELECT VALUE (WITH x AS (SELECT VALUE o.lo FROM << 0 >> AS z) SELECT VALUE v FROM x AS v) FROM << { 'lo': 1 } >> AS o",
                OUTER_REFERENCE,
            ),
            Arguments.of(
                "reference to a variable of an enclosing query within a subquery of the element",
                "SELECT VALUE (WITH x AS (SELECT VALUE (SELECT VALUE o.lo FROM << 0 >> AS y) FROM << 0 >> AS z) SELECT VALUE v FROM x AS v) FROM << { 'lo': 1 } >> AS o",
                OUTER_REFERENCE,
            ),
            Arguments.of(
                "lateral join",
                "WITH x AS (SELECT VALUE c FROM << { 'children': [1, 2] } >> AS p, p.children AS c) SELECT VALUE v FROM x AS v",
                LATERAL,
            ),
        )

        @JvmStatic
        fun unsupported() = unsupportedCases

        @JvmStatic
        fun unsupportedQueries() = unsupportedCases.map { it.get()[1] }

        @JvmStatic
        fun supported() = listOf(
            Arguments.of(
                "element within a subquery without outer references",
                "SELECT VALUE (WITH x AS (SELECT VALUE z FROM << 1 >> AS z) SELECT VALUE v FROM x AS v) FROM << { 'lo': 1 } >> AS o",
            ),
            Arguments.of(
                "element referencing an element of an enclosing WITH clause",
                "WITH x AS (SELECT VALUE z FROM << 1 >> AS z) SELECT VALUE (WITH y AS (SELECT VALUE v FROM x AS v) SELECT VALUE w FROM y AS w) FROM << 0 >> AS o",
            ),
            Arguments.of(
                "element referencing a variable of its own subquery",
                "WITH x AS (SELECT VALUE (SELECT VALUE z + y FROM << 1 >> AS y) FROM << 1 >> AS z) SELECT VALUE v FROM x AS v",
            ),
            Arguments.of(
                "element containing a join",
                "WITH x AS (SELECT a.n AS n FROM << { 'n': 1 } >> AS a INNER JOIN << { 'n': 1 } >> AS b ON a.n = b.n) SELECT VALUE v FROM x AS v",
            ),
            Arguments.of(
                "lateral join over an element",
                "WITH x AS (SELECT VALUE p FROM << { 'children': [1, 2] } >> AS p) SELECT VALUE c FROM x AS v, v.children AS c",
            ),
        )
    }
}

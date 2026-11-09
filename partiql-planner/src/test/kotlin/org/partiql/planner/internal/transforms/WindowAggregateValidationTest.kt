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

package org.partiql.planner.internal.transforms

import org.junit.jupiter.api.Test
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.Statement
import org.partiql.ast.WindowFunctionType
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.util.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.Severity
import kotlin.test.assertEquals

/**
 * Validation of aggregate window functions in the planner. Names the parser cannot produce are built by rewriting the
 * parsed AST.
 */
@Suppress("DEPRECATION")
class WindowAggregateValidationTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val session = Session.builder()
        .catalog("default")
        .catalogs(Catalog.builder().name("default").build())
        .build()

    private val data = "<< { 'k': 1, 'x': 1 }, { 'k': 2, 'x': 2 } >>"

    private fun errors(statement: Statement): List<PError> {
        val collector = PErrorCollector()
        planner.plan(statement, session, Context.of(collector))
        return collector.problems.filter { it.severity.code() == Severity.ERROR }
    }

    private fun errors(query: String): List<PError> = errors(parser.parse(query).statements.single())

    /**
     * Parses [query] and replaces the name of its aggregate window function with [function].
     */
    private fun withAggregateName(query: String, function: Identifier): Statement {
        val statement = parser.parse(query).statements.single()
        val rewriter = object : AstRewriter<Unit>() {
            override fun visitWindowFunctionTypeAggregate(node: WindowFunctionType.Aggregate, ctx: Unit): AstNode =
                WindowFunctionType.Aggregate(function, node.args, node.setq)
        }
        return rewriter.visitStatement(statement, Unit) as Statement
    }

    private fun assertSingleFeatureNotSupported(errors: List<PError>, featureName: String) {
        assertEquals(listOf(PError.FEATURE_NOT_SUPPORTED), errors.map { it.code() }, "Errors: $errors")
        assertEquals(featureName, errors.single().getOrNull("FEATURE_NAME", String::class.java))
    }

    @Test
    fun qualifiedAggregateIsNotSupported() {
        val statement = withAggregateName("SELECT SUM(t.x) OVER (ORDER BY t.k) AS s FROM $data AS t", Identifier.regular("default", "sum"))
        assertSingleFeatureNotSupported(errors(statement), "Aggregate default.sum as a window function")
    }

    @Test
    fun unknownAggregateIsNotSupported() {
        val statement = withAggregateName("SELECT SUM(t.x) OVER (ORDER BY t.k) AS s FROM $data AS t", Identifier.regular("foo"))
        assertSingleFeatureNotSupported(errors(statement), "Aggregate foo as a window function")
    }

    @Test
    fun internalAggregateIsNotSupported() {
        val statement = withAggregateName("SELECT SUM(t.x) OVER () AS s FROM $data AS t", Identifier.regular("group_as"))
        assertSingleFeatureNotSupported(errors(statement), "Aggregate group_as as a window function")
    }

    @Test
    fun unknownAggregateWithDistinctAndOrderByReportsOnlyTheName() {
        val statement = withAggregateName("SELECT SUM(DISTINCT t.x) OVER (ORDER BY t.k) AS s FROM $data AS t", Identifier.regular("FOO"))
        assertSingleFeatureNotSupported(errors(statement), "Aggregate FOO as a window function")
    }

    @Test
    fun delimitedAggregateNameMatchesExactly() {
        val query = "SELECT SUM(t.x) OVER (ORDER BY t.k) AS s FROM $data AS t"
        assertEquals(emptyList(), errors(withAggregateName(query, Identifier.delimited("sum"))))
        assertSingleFeatureNotSupported(
            errors(withAggregateName(query, Identifier.delimited("SUM"))),
            "Aggregate \"SUM\" as a window function"
        )
    }

    @Test
    fun distinctWithOrderByReportsExactlyOneProblem() {
        listOf(
            "SELECT t.k, SUM(DISTINCT t.k) OVER (ORDER BY t.k) AS s FROM $data AS t" to PError.INVALID_WINDOW_FUNCTION,
            "SELECT t.k, COUNT(DISTINCT t.k) OVER (ORDER BY t.k) AS c FROM $data AS t" to PError.FEATURE_NOT_SUPPORTED,
        ).forEach { (query, code) ->
            val collector = PErrorCollector()
            planner.plan(parser.parse(query).statements.single(), session, Context.of(collector))
            val problems = collector.problems.filter { it.code() != PError.EXPERIMENTAL }
            assertEquals(listOf(code), problems.map { it.code() }, "Problems for $query: $problems")
        }
    }
}

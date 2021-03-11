/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.metaContainerOf
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.AggregateCallSiteListMeta
import org.partiql.lang.ast.AggregateRegisterIdMeta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.ArgumentsProviderBase

class AggregateSupportVisitorTransformTests : VisitorTransformTestBase() {
    private val transformer = AggregateSupportVisitorTransform()

    data class AggSupportTestCase(val query: String, val expectedCallAggs: List<Pair<String, Int>>)

    /**
     * Simple helper for testing that parses [this] SFW query, transforms it using [AggregateSupportVisitorTransform],
     * and returns the transformed query as [PartiqlAst.Expr.Select].
     */
    private fun String.parseAndTransformQuery() : PartiqlAst.Expr.Select {
        val query = this
        val statement = super.parser.parseExprNode(query).toAstStatement()
        val transformedNode = (transformer).transformStatement(statement) as PartiqlAst.Statement.Query
        return (transformedNode.expr) as PartiqlAst.Expr.Select
    }

    /**
     * Simple helper for testing to create a [MetaContainer] with [AggregateCallSiteListMeta] mapping to a list of
     * [PartiqlAst.Expr.CallAgg]s.
     *
     * Each of the created [PartiqlAst.Expr.CallAgg]s uses the function name provided by [callAggs]'s first argument
     * and [AggregateRegisterIdMeta] passed as the second argument of the pair.
     */
    private fun createCallAggMetas(callAggs: List<Pair<String, Int>>): MetaContainer =
        metaContainerOf(AggregateCallSiteListMeta.TAG to AggregateCallSiteListMeta(
            callAggs.map { callAgg ->
                PartiqlAst.build {
                    callAgg(
                        setq = all(),
                        funcName = callAgg.first,
                        arg = lit(ionInt(1)),
                        metas = metaContainerOf(AggregateRegisterIdMeta.TAG to AggregateRegisterIdMeta(callAgg.second)))
                }
            }))

    /**
     * Simple helper for testing to remove the [SourceLocationMeta] from [this] [MetaContainer].
     */
    private fun MetaContainer.removeSourceLocation() : MetaContainer = this.minus(SourceLocationMeta.TAG)

    /**
     * Checks that [expected] and [actual] have the same metas (i.e. [AggregateCallSiteListMeta])
     * (excluding source location).
     */
    private fun assertSameCallAggMetas(expected: MetaContainer, actual: MetaContainer) {
        val expectedAggCalls = (expected[AggregateCallSiteListMeta.TAG] as AggregateCallSiteListMeta).aggregateCallSites
        val actualAggCalls = (actual[AggregateCallSiteListMeta.TAG] as AggregateCallSiteListMeta).aggregateCallSites
        expectedAggCalls.zip(actualAggCalls).forEach { (e, a) ->
            assertEquals(e, a)

            // check that these metas have the same [AggregateRegisterIdMeta]
            assertEquals(e.metas, a.metas.removeSourceLocation())
        }
    }

    class NonSubqueryCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // one aggregate transform
            AggSupportTestCase(
                "SELECT COUNT(1) FROM foo",
                listOf(Pair("count", 0))),
            // multiple aggregates transform
            AggSupportTestCase(
                "SELECT COUNT(1), SUM(1), AVG(1) FROM foo",
                listOf(Pair("count", 0), Pair("sum", 1), Pair("avg", 2))),
            // one aggregate in HAVING transform
            AggSupportTestCase(
                "SELECT 1 FROM foo GROUP BY bar HAVING SUM(1) > 0",
                listOf(Pair("sum", 0))),
            // one aggregate and one aggregate in HAVING transform
            AggSupportTestCase(
                "SELECT COUNT(1) FROM foo GROUP BY bar HAVING SUM(1) > 0",
                listOf(Pair("sum", 0), Pair("count", 1))),
            // SELECT VALUE aggregate transform
            AggSupportTestCase(
                "SELECT VALUE COUNT(1) FROM foo",
                emptyList()),
            // SELECT VALUE one aggregate and HAVING transform
            AggSupportTestCase(
                "SELECT VALUE COUNT(1) FROM foo GROUP BY bar HAVING SUM(1) > 0",
                listOf(Pair("sum", 0)))
        )
    }

    @ParameterizedTest
    @ArgumentsSource(NonSubqueryCases::class)
    fun testNonSubquery(tc: AggSupportTestCase) {
        val select = tc.query.parseAndTransformQuery()

        val actualMetas = select.metas
        val expectedMetas = createCallAggMetas(tc.expectedCallAggs)

        assertSameCallAggMetas(expectedMetas, actualMetas)
    }

    @Test
    fun `SELECT VALUE with SELECT VALUE subquery aggregate transform`() {
        val query = "SELECT VALUE (SELECT VALUE COUNT(1) FROM foo) FROM foo"

        // outer SELECT VALUE has no aggregate call sites
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(emptyList())

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner SELECT VALUE has no aggregate call sites
        val innerSelect = (outerSelect.project as PartiqlAst.Projection.ProjectValue).value as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(emptyList())

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)
    }

    @Test
    fun `SELECT VALUE with subquery aggregate transform`() {
        val query = "SELECT VALUE (SELECT COUNT(1) FROM foo) FROM foo"

        // outer SELECT VALUE has no aggregate call sites
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(emptyList())

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner SELECT query has 1 aggregate call
        val innerSelect = (outerSelect.project as PartiqlAst.Projection.ProjectValue).value as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(listOf(Pair("count", 0)))

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)
    }

    @Test
    fun `FROM clause subquery aggregate transform`() {
        val query = "SELECT 1 FROM (SELECT COUNT(1), SUM(1) FROM foo)"

        // outer query has no aggregate calls
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(emptyList())

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner query has 2 aggregate calls
        val innerSelect = (outerSelect.from as PartiqlAst.FromSource.Scan).expr as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(listOf(Pair("count", 0), Pair("sum", 1)))

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)
    }

    @Test
    fun `FROM clause subquery aggregate transform with HAVING`() {
        val query = "SELECT 1 FROM (SELECT COUNT(1), SUM(1) FROM foo) GROUP BY bar HAVING SUM(1) > 0"

        // outer query has 1 aggregate call
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(listOf(Pair("sum", 0)))

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner query has 2 aggregate calls
        val innerSelect = (outerSelect.from as PartiqlAst.FromSource.Scan).expr as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(listOf(Pair("count", 0), Pair("sum", 1)))

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)
    }

    @Test
    fun `FROM clause subsubquery aggregate transform`() {
        val query = "SELECT 1 FROM (SELECT COUNT(1), SUM(1) FROM (SELECT AVG(1) FROM foo))"

        // outer query has no aggregate calls
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(emptyList())

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner query has 2 aggregate calls
        val innerSelect = (outerSelect.from as PartiqlAst.FromSource.Scan).expr as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(listOf(Pair("count", 0), Pair("sum", 1)))

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)

        // innermost query has 1 aggregate calls
        val innermostSelect = (innerSelect.from as PartiqlAst.FromSource.Scan).expr as PartiqlAst.Expr.Select
        val innermostActualMetas = innermostSelect.metas
        val innermostExpectedMetas = createCallAggMetas(listOf(Pair("avg", 0)))

        assertSameCallAggMetas(innermostExpectedMetas, innermostActualMetas)
    }

    @Test
    fun `SELECT VALUE subquery aggregate transform`() {
        val query = "SELECT VALUE AVG(1) FROM (SELECT COUNT(1), SUM(1) FROM foo)"

        // outer SELECT VALUE isn't included as an aggregate call site
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(emptyList())

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner query has 2 aggregate calls
        val innerSelect = (outerSelect.from as PartiqlAst.FromSource.Scan).expr as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(listOf(Pair("count", 0), Pair("sum", 1)))

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)
    }

    @Test
    fun `SELECT clause subquery aggregate transform`() {
        val query = "SELECT AVG(1), (SELECT COUNT(1), SUM(1) FROM foo) FROM foo"

        // outer query has 1 aggregate call
        val outerSelect = query.parseAndTransformQuery()
        val outerActualMetas = outerSelect.metas
        val outerExpectedMetas = createCallAggMetas(listOf(Pair("avg", 0)))

        assertSameCallAggMetas(outerExpectedMetas, outerActualMetas)

        // inner select query's second item has 2 aggregate calls
        val innerSelectList = (outerSelect.project as PartiqlAst.Projection.ProjectList).projectItems
        val innerSelect = (innerSelectList[1] as PartiqlAst.ProjectItem.ProjectExpr).expr as PartiqlAst.Expr.Select
        val innerActualMetas = innerSelect.metas
        val innerExpectedMetas = createCallAggMetas(listOf(Pair("count", 0), Pair("sum", 1)))

        assertSameCallAggMetas(innerExpectedMetas, innerActualMetas)
    }
}

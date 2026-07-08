/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file.
 */

package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.NormalizeFromSource
import org.partiql.planner.internal.transforms.NormalizeGroupBy
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PErrorListener

/**
 * Tests verifying that the planner correctly classifies joins as correlated (Rel.Op.Correlate)
 * or non-correlated (Rel.Op.Join) based on whether the RHS references the LHS scope.
 *
 * Each test demonstrates a specific scenario that exercises the hasOuterReference walk,
 * particularly the nesting counter that tracks scope boundaries (Select, Subquery, Pivot).
 */
internal class CorrelatedJoinClassificationTest {

    private val parser = PartiQLParser.standard()

    private fun plan(query: String): Statement {
        val parseResult = parser.parse(query)
        val stmt = parseResult.statements[0]
        val catalog = Catalog.builder().name("memory").build()
        val session = Session.builder().catalog("memory").catalogs(catalog).build()
        val env = Env(session, PErrorListener.abortOnError())
        val normalized = NormalizeFromSource.apply(stmt)
        val normalized2 = NormalizeGroupBy.apply(normalized)
        val root = AstToPlan.apply(normalized2, env)
        val typer = PlanTyper(env, Context.of(PErrorListener.abortOnError()), emptySet())
        return typer.resolve(root)
    }

    private fun findFirstJoinOrCorrelate(node: PlanNode): Rel.Op? {
        if (node is Rel) {
            if (node.op is Rel.Op.Join || node.op is Rel.Op.Correlate) {
                return node.op
            }
        }
        for (child in node.children) {
            val found = findFirstJoinOrCorrelate(child)
            if (found != null) return found
        }
        return null
    }

    private fun assertCorrelate(stmt: Statement, msg: String) {
        val op = findFirstJoinOrCorrelate(stmt)
        assert(op is Rel.Op.Correlate) { "$msg — expected Correlate but got ${op?.javaClass?.simpleName}" }
    }

    private fun assertJoin(stmt: Statement, msg: String) {
        val op = findFirstJoinOrCorrelate(stmt)
        assert(op is Rel.Op.Join) { "$msg — expected Join but got ${op?.javaClass?.simpleName}" }
    }

    // ==========================================================================
    // Non-correlated: should produce Rel.Op.Join
    // ==========================================================================

    @Test
    fun `non-correlated - independent collections`() {
        val stmt = plan("SELECT VALUE [a, b] FROM << 1, 2 >> AS a INNER JOIN << 10, 20 >> AS b ON a = b")
        assertJoin(stmt, "Two independent collections with ON condition")
    }

    @Test
    fun `non-correlated - cross join of literals`() {
        val stmt = plan("SELECT VALUE [a, b] FROM << 1, 2 >> AS a, << 10, 20 >> AS b")
        assertJoin(stmt, "Cross join of independent literals")
    }

    @Test
    fun `non-correlated - subquery not referencing LHS`() {
        val stmt = plan(
            "SELECT VALUE x FROM << {'id': 1} >> AS t INNER JOIN (SELECT VALUE i FROM << 1, 2, 3 >> AS i WHERE i > 1) AS x ON true"
        )
        assertJoin(stmt, "Subquery in RHS does not reference LHS")
    }

    @Test
    fun `non-correlated - subquery references outer query not LHS`() {
        // The subquery inside the join references t1 from the enclosing SELECT, NOT the join's LHS 'a'.
        // This exercises nesting > 0: inside the subquery, t1 is at depth=3 and nesting=1,
        // so depth != nesting + 1 (3 != 2), correctly classified as non-correlated.
        val stmt = plan(
            "SELECT VALUE (SELECT VALUE x FROM << 1, 2 >> AS a INNER JOIN (SELECT VALUE i FROM << 1, 2, 3 >> AS i WHERE i <= t1) AS x ON true) FROM << 2 >> AS t1"
        )
        assertJoin(stmt, "Subquery refs outer query scope, not the join's LHS")
    }

    // ==========================================================================
    // Correlated: should produce Rel.Op.Correlate
    // ==========================================================================

    @Test
    fun `correlated - path lateral reference (nesting=0)`() {
        // Direct path on LHS alias. The scan rex 't.items' has depth=1 at nesting=0.
        // depth == 0 + 1 → correlated.
        val stmt = plan("SELECT VALUE x FROM << {'items': [1, 2]} >> AS t INNER JOIN t.items AS x ON true")
        assertCorrelate(stmt, "Path lateral: t.items references LHS at nesting=0")
    }

    @Test
    fun `correlated - implicit path lateral reference (nesting=0)`() {
        val stmt = plan("SELECT VALUE x FROM << {'items': [1, 2]} >> AS t INNER JOIN items AS x ON true")
        assertCorrelate(stmt, "Path lateral: t.items references LHS at nesting=0")
    }

    @Test
    fun `correlated - path lateral with ON condition`() {
        // Path lateral with non-trivial ON condition. Condition should be pushed as Filter on RHS.
        val stmt = plan(
            "SELECT VALUE x FROM << [0, 1, 2], [10, 11, 12] >> AS lhs INNER JOIN lhs AS rhs ON lhs[2] = rhs"
        )
        assertCorrelate(stmt, "Path lateral with ON condition pushed to RHS filter")
    }

    @Test
    fun `correlated - subquery referencing LHS (nesting=1, Select boundary)`() {
        // Subquery in RHS references 't.n'. Inside the Select, t.n resolves at depth=2.
        // nesting=1 (one Select boundary), so depth == 1 + 1 → correlated.
        val stmt = plan(
            "SELECT VALUE x FROM << {'id': 1, 'n': 2} >> AS t INNER JOIN (SELECT VALUE i FROM << 1, 2, 3 >> AS i WHERE i <= t.n) AS x ON true"
        )
        assertCorrelate(stmt, "Correlated subquery: depth=2 at nesting=1 (Select boundary)")
    }

    @Test
    fun `correlated - scalar subquery referencing LHS (nesting=2, Select+Subquery boundaries)`() {
        // Scalar subquery (Rex.Op.Subquery) inside a Select. The ref to t.n crosses two boundaries.
        // nesting=2 (Select + Subquery), depth=3, so depth == 2 + 1 → correlated.
        val stmt = plan(
            "SELECT VALUE x FROM << {'n': 5} >> AS t INNER JOIN (SELECT VALUE (SELECT t.n FROM << 1 >> AS dummy) FROM << 1 >> AS i) AS x ON true"
        )
        assertCorrelate(stmt, "Scalar subquery: depth=3 at nesting=2 (Select + Subquery boundaries)")
    }

    @Test
    fun `correlated - nested subquery both referencing LHS`() {
        // Two levels of subqueries, both referencing t.n from the join's LHS.
        // Outer subquery: nesting=1, depth=2 → 2 == 1+1 ✓
        // Inner subquery: nesting=2, depth=3 → 3 == 2+1 ✓
        val stmt = plan(
            "SELECT VALUE x FROM << {'n': 3} >> AS t INNER JOIN (SELECT VALUE (SELECT VALUE j FROM << 1 >> AS j WHERE j <= t.n) FROM << 1, 2, 3 >> AS i WHERE i <= t.n) AS x ON true"
        )
        assertCorrelate(stmt, "Nested subqueries both referencing LHS at different nesting levels")
    }

    @Test
    fun `correlated - LEFT JOIN path lateral`() {
        // LEFT JOIN with path lateral. Should produce Correlate with LEFT type.
        val stmt = plan("SELECT VALUE [t.name, x] FROM << {'name': 'a', 'vals': [1, 2]} >> AS t LEFT JOIN t.vals AS x ON true")
        val op = findFirstJoinOrCorrelate(stmt)
        assert(op is Rel.Op.Correlate) { "Expected Correlate for LEFT lateral" }
        assert((op as Rel.Op.Correlate).type == Rel.Op.Correlate.Type.LEFT) { "Expected LEFT type" }
    }

    // ==========================================================================
    // Verify ON condition is pushed as Filter for correlated joins
    // ==========================================================================

    @Test
    fun `correlated - ON condition becomes Filter on RHS`() {
        // ON lhs[2] = rhs should be pushed into the RHS as a Filter wrapping the scan.
        val stmt = plan(
            "SELECT VALUE rhs FROM << [0, 1, 2], [10, 11, 12] >> AS lhs INNER JOIN lhs AS rhs ON lhs[2] = rhs"
        )
        val op = findFirstJoinOrCorrelate(stmt) as Rel.Op.Correlate
        // The RHS should be a Filter wrapping a Scan
        assert(op.rhs.op is Rel.Op.Filter) {
            "Expected RHS to be Filter(Scan(...)) but got ${op.rhs.op.javaClass.simpleName}"
        }
    }

    @Test
    fun `correlated - trivial ON TRUE still wraps Filter`() {
        // ON true is still wrapped as Filter (optimization to remove it is a separate pass).
        val stmt = plan("SELECT VALUE x FROM << {'items': [1]} >> AS t INNER JOIN t.items AS x ON true")
        val op = findFirstJoinOrCorrelate(stmt) as Rel.Op.Correlate
        assert(op.rhs.op is Rel.Op.Filter) {
            "Expected RHS to be Filter (condition always pushed) but got ${op.rhs.op.javaClass.simpleName}"
        }
    }

    @Test
    fun `correlated - comma join no ON condition`() {
        // FROM t, t.items AS x — no explicit ON clause. Parser generates ON TRUE implicitly.
        val stmt = plan("SELECT VALUE x FROM << {'items': [1, 2]} >> AS t, t.items AS x")
        assertCorrelate(stmt, "Comma lateral join with no ON condition")
    }

    @Test
    fun `non-correlated - comma join no ON condition`() {
        // FROM a, b — no ON, no correlation
        val stmt = plan("SELECT VALUE [a, b] FROM << 1 >> AS a, << 2 >> AS b")
        assertJoin(stmt, "Comma join of independent collections with no ON")
    }

    // ==========================================================================
    // Multiple joins
    // ==========================================================================

    @Test
    fun `multiple joins - chain of correlated`() {
        // FROM t, t.a AS a, a.b AS b — both inner joins are correlated
        // The outermost join (result, b) should be Correlate
        val stmt = plan("SELECT VALUE b FROM << {'a': [{'b': [1, 2]}]} >> AS t, t.a AS a, a.b AS b")
        val op = findFirstJoinOrCorrelate(stmt)
        assert(op is Rel.Op.Correlate) { "Outermost join in chain should be Correlate" }
    }

    @Test
    fun `multiple joins - mixed correlated and non-correlated`() {
        // FROM t, << 1, 2 >> AS a, t.items AS x
        // First join (t, a) is non-correlated. Second join ((t,a), x) is correlated.
        val stmt = plan("SELECT VALUE x FROM << {'items': [10]} >> AS t, << 1, 2 >> AS a, t.items AS x")
        // The outermost join should be Correlate (x references t from LHS)
        val op = findFirstJoinOrCorrelate(stmt)
        assert(op is Rel.Op.Correlate) { "Outermost join should be Correlate (x refs t)" }
        // The inner join should be Join (a is independent)
        val innerOp = (op as Rel.Op.Correlate).lhs.op
        assert(innerOp is Rel.Op.Join) { "Inner join should be Join (a is independent) but got ${innerOp.javaClass.simpleName}" }
    }

    // ==========================================================================
    // Scope boundaries: Select, Subquery, Pivot
    // ==========================================================================

    @Test
    fun `scope boundary - Rex_Op_Select (SELECT VALUE subquery)`() {
        // The correlated ref crosses one Select boundary (nesting=1, depth=2)
        val stmt = plan(
            "SELECT VALUE x FROM << {'n': 2} >> AS t INNER JOIN (SELECT VALUE i FROM << 1, 2, 3 >> AS i WHERE i <= t.n) AS x ON true"
        )
        assertCorrelate(stmt, "Select boundary: correlated ref at depth=2, nesting=1")
    }

    @Test
    fun `scope boundary - Rex_Op_Subquery (scalar subquery)`() {
        // The correlated ref crosses Select + Subquery boundaries (nesting=2, depth=3)
        val stmt = plan(
            "SELECT VALUE x FROM << {'n': 5} >> AS t INNER JOIN (SELECT VALUE (SELECT t.n FROM << 1 >> AS dummy) FROM << 1 >> AS i) AS x ON true"
        )
        assertCorrelate(stmt, "Subquery boundary: correlated ref at depth=3, nesting=2")
    }

    @Test
    fun `scope boundary - non-correlated despite deep nesting`() {
        // Deeply nested subquery but references are all local — not correlated
        val stmt = plan(
            "SELECT VALUE x FROM << 1 >> AS t INNER JOIN (SELECT VALUE (SELECT VALUE j + i FROM << 1 >> AS j) FROM << 1, 2 >> AS i) AS x ON true"
        )
        assertJoin(stmt, "Deep nesting but all refs are local — non-correlated")
    }
}

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
 *
 */

package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.FromExpr
import org.partiql.ast.FromJoin
import org.partiql.ast.Identifier
import org.partiql.ast.JoinType
import org.partiql.ast.QueryBody
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.WithListElement
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep

/**
 * Syntactic analysis of a `WITH RECURSIVE` clause (SQL:2023 Part 2, Subclause 7.17, Syntax Rule 3).
 *
 * A WITH list element (query) name is referenced when it is the root of a FROM source, e.g. `FROM x AS a` or
 * `FROM x.children AS c`. This mirrors how WITH list elements are resolved by the typer.
 *
 * A query violating the syntax rules is reported as invalid, whereas a valid query that is not (yet) supported is
 * reported as such; see [Violation].
 */
internal object RecursiveWith {

    /**
     * A recursive WITH list element split into its non-recursive (seed) and recursive operands of the top-level UNION.
     */
    internal class Operands(
        val seeds: List<Expr>,
        val recursives: List<Expr>,
        val setq: SetQuantifier?,
    )

    /**
     * Thrown for a recursive query that cannot be planned.
     *
     * @property message describes the violated syntax rule, or the unsupported feature if [unsupported].
     * @property unsupported true if the query is valid, but uses a feature that is not supported.
     */
    internal class Violation(override val message: String, val unsupported: Boolean = false) : Exception(message)

    private fun invalid(message: String): Nothing = throw Violation(message)

    private fun unsupported(feature: String): Nothing = throw Violation(feature, unsupported = true)

    /**
     * Returns true if [name] (a WITH list element name) is referenced anywhere within [node].
     */
    internal fun references(node: AstNode, name: Identifier.Simple): Boolean = sites(node, name).isNotEmpty()

    /**
     * Returns true if the query names [a] and [b] are equivalent; a regular (unquoted) name is case-insensitive.
     */
    internal fun equivalent(a: Identifier.Simple, b: Identifier.Simple): Boolean =
        a.text.equals(b.text, ignoreCase = a.isRegular || b.isRegular)

    /**
     * Returns true if [node] contains a WITH RECURSIVE clause (SQL:2023 Part 2, Subclause 7.17, Syntax Rule 3.h).
     */
    internal fun containsRecursiveWith(node: AstNode): Boolean {
        var found = false
        val visitor = object : AstVisitor<Unit, Unit>() {
            override fun defaultReturn(node: AstNode, ctx: Unit) = Unit
            override fun visitExprQuerySet(node: ExprQuerySet, ctx: Unit) {
                if (node.with?.isRecursive == true) {
                    found = true
                    return
                }
                defaultVisit(node, ctx)
            }
        }
        node.accept(visitor, Unit)
        return found
    }

    /**
     * Returns the indices of the elements (in [elements]) that are referenced from within [node].
     */
    internal fun dependencies(node: AstNode, elements: List<WithListElement>): Set<Int> =
        elements.indices.filter { references(node, elements[it].queryName) }.toSet()

    /**
     * Splits the query of a recursive WITH list element into the operands of its top-level UNION [ALL]. Operands of
     * nested UNIONs having the same set quantifier are flattened, e.g. `a UNION ALL b UNION ALL c` has three operands.
     *
     * The supported form is `<non-recursive query> UNION [ALL] <recursive query>`, where each recursive operand is a
     * SELECT query. Other valid forms (SQL:2023 Part 2, Subclause 7.17, Syntax Rule 3.j) are reported as unsupported.
     */
    internal fun split(element: WithListElement): Operands {
        val query = element.asQuery
        val name = element.queryName
        // Syntax Rule 3.j.ix.3; LIMIT is the PartiQL equivalent of <fetch first clause>.
        if (query.limit != null) {
            invalid("LIMIT is not allowed")
        }
        if (query.with != null) {
            unsupported("WITH clause within a recursive WITH list element")
        }
        if (query.orderBy != null) {
            unsupported("ORDER BY on a recursive WITH list element")
        }
        if (query.offset != null) {
            unsupported("OFFSET on a recursive WITH list element")
        }
        // Syntax Rule 3.j.vii: the query must contain a UNION having a non-recursive (anchor) operand.
        if (!hasAnchor(query.body, name)) {
            invalid("the query must be of the form <non-recursive query> UNION [ALL] <recursive query>")
        }
        val body = query.body as QueryBody.SetOp
        if (body.type.setOpType.code() != SetOpType.UNION || body.isOuter) {
            checkSetOps(query, name)
            unsupported("recursive query whose top-level set operator is not UNION")
        }
        val setq = body.type.setq
        val operands = mutableListOf<Expr>()
        flatten(body, setq?.code(), operands)
        val (recursives, seeds) = operands.partition { references(it, name) }
        if (seeds.isEmpty()) {
            // The anchor is a nested UNION, e.g. `(a UNION ALL b) UNION c` where `b` and `c` are recursive.
            operands.forEach { checkSetOps(it, name) }
            unsupported("recursive query whose non-recursive operand is within a nested set operation")
        }
        for (operand in recursives) {
            if (operand is ExprQuerySet && operand.body is QueryBody.SFW) {
                validate(operand, name)
            } else {
                checkSetOps(operand, name)
                unsupported("recursive operand that is not a SELECT query")
            }
        }
        return Operands(seeds, recursives, setq)
    }

    /**
     * Returns true if [body] is, or simply contains, a UNION having an operand that does not reference [name].
     */
    private fun hasAnchor(body: QueryBody, name: Identifier.Simple): Boolean {
        if (body !is QueryBody.SetOp) {
            return false
        }
        if (body.type.setOpType.code() == SetOpType.UNION && (!references(body.lhs, name) || !references(body.rhs, name))) {
            return true
        }
        return listOf(body.lhs, body.rhs).any { operand ->
            val nested = simplyContainedBody(operand)
            nested != null && hasAnchor(nested, name)
        }
    }

    /**
     * Returns the body of [operand] if it is a <query primary> (i.e. not a nested <query expression> with a WITH).
     */
    private fun simplyContainedBody(operand: Expr): QueryBody? =
        (operand as? ExprQuerySet)?.takeIf { it.with == null }?.body

    /**
     * Validates the set operations and SELECT queries that reference [name] within [expr] (SQL:2023 Part 2,
     * Subclause 7.17, Syntax Rules 3.j.ix.5.D and 3.j.ix.5.E).
     */
    private fun checkSetOps(expr: Expr, name: Identifier.Simple) {
        if (expr !is ExprQuerySet || !references(expr, name)) {
            return
        }
        when (val body = expr.body) {
            is QueryBody.SFW -> validate(expr, name)
            is QueryBody.SetOp -> {
                val type = body.type.setOpType.code()
                val all = body.type.setq?.code() == SetQuantifier.ALL
                if (all && (type == SetOpType.INTERSECT || type == SetOpType.EXCEPT)) {
                    invalid("the recursive reference must not appear within INTERSECT ALL or EXCEPT ALL")
                }
                if (type == SetOpType.EXCEPT && references(body.rhs, name)) {
                    invalid("the recursive reference must not appear in the right operand of EXCEPT")
                }
                checkSetOps(body.lhs, name)
                checkSetOps(body.rhs, name)
            }
            else -> Unit
        }
    }

    private fun flatten(body: QueryBody.SetOp, setq: Int?, operands: MutableList<Expr>) {
        val lhs = body.lhs
        val lhsBody = (lhs as? ExprQuerySet)?.body
        val flattenable = lhs is ExprQuerySet &&
            lhs.with == null && lhs.orderBy == null && lhs.limit == null && lhs.offset == null &&
            lhsBody is QueryBody.SetOp && !lhsBody.isOuter &&
            lhsBody.type.setOpType.code() == SetOpType.UNION &&
            lhsBody.type.setq?.code() == setq
        if (flattenable) {
            flatten(lhsBody as QueryBody.SetOp, setq, operands)
        } else {
            operands.add(lhs)
        }
        operands.add(body.rhs)
    }

    /**
     * Validates a SELECT query that references [name] (SQL:2023 Part 2, Subclause 7.17, Syntax Rules 3.j.ix and
     * 3.j.x). Aggregate and window functions are checked after the conversion to a plan, see RelConverter.
     */
    private fun validate(operand: ExprQuerySet, name: Identifier.Simple) {
        // A parenthesized operand having a WITH clause is a nested <query expression> (Syntax Rule 3.j.ix.5.B).
        if (operand.with != null) {
            invalid("the recursive reference must not appear within a subquery")
        }
        // Syntax Rule 3.j.ix.4.
        if (operand.limit != null) {
            invalid("the recursive reference must not appear in a query having a LIMIT")
        }
        if (operand.orderBy != null) {
            unsupported("ORDER BY on a recursive operand")
        }
        if (operand.offset != null) {
            unsupported("OFFSET on a recursive operand")
        }
        val sites = sites(operand, name)
        if (sites.size > 1) {
            invalid("the recursive reference must appear at most once in a recursive operand (linear recursion)")
        }
        val site = sites.single()
        if (site.nested) {
            invalid("the recursive reference must not appear within a subquery")
        }
        if (site.nullable) {
            invalid("the recursive reference must not appear on the null-producing side of an outer join")
        }
        if (site.derived) {
            unsupported("recursive reference within a derived table")
        }
        if (site.wildcard) {
            unsupported("path with wildcard steps over a recursive reference")
        }
    }

    /**
     * The position of a reference to a WITH list element.
     *
     * @property nested true if the reference is within a nested query expression (subquery), excluding [derived].
     * @property derived true if the reference is within a derived table of the FROM clause of the top-level query.
     * @property nullable true if the reference is on the null-producing side of an outer join.
     * @property wildcard true if the reference is the root of a path with wildcard steps, e.g. `FROM x[*].children[*]`.
     */
    private data class Site(val nested: Boolean, val derived: Boolean, val nullable: Boolean, val wildcard: Boolean = false)

    private fun sites(node: AstNode, name: Identifier.Simple): List<Site> {
        val collector = SiteCollector(name)
        val ctx = Site(nested = false, derived = false, nullable = false)
        when (node) {
            // The top-level query of the given node is not considered nested.
            is ExprQuerySet -> node.children.forEach { it.accept(collector, ctx) }
            else -> node.accept(collector, ctx)
        }
        return collector.sites
    }

    private class SiteCollector(private val name: Identifier.Simple) : AstVisitor<Unit, Site>() {

        val sites = mutableListOf<Site>()

        override fun defaultReturn(node: AstNode, ctx: Site) = Unit

        override fun visitExprQuerySet(node: ExprQuerySet, ctx: Site) {
            defaultVisit(node, ctx.copy(nested = true))
        }

        override fun visitFromJoin(node: FromJoin, ctx: Site) {
            val (lhsNullable, rhsNullable) = when (node.joinType?.code()) {
                JoinType.LEFT, JoinType.LEFT_OUTER, JoinType.LEFT_CROSS -> false to true
                JoinType.RIGHT, JoinType.RIGHT_OUTER, JoinType.RIGHT_CROSS -> true to false
                JoinType.FULL, JoinType.FULL_OUTER, JoinType.FULL_CROSS -> true to true
                else -> false to false
            }
            node.lhs.accept(this, ctx.copy(nullable = ctx.nullable || lhsNullable))
            node.rhs.accept(this, ctx.copy(nullable = ctx.nullable || rhsNullable))
            node.condition?.accept(this, ctx)
        }

        override fun visitFromExpr(node: FromExpr, ctx: Site) {
            val expr = node.expr
            if (isReference(expr)) {
                sites.add(ctx.copy(wildcard = expr.hasWildcard()))
            }
            if (expr is ExprQuerySet && !ctx.nested && !ctx.derived) {
                // A derived table of the top-level query; its own query is not considered nested.
                expr.children.forEach { it.accept(this, ctx.copy(derived = true)) }
            } else {
                defaultVisit(node, ctx)
            }
        }

        private fun Expr.hasWildcard(): Boolean =
            this is ExprPath && (steps.any { it is PathStep.AllElements || it is PathStep.AllFields } || root.hasWildcard())

        private fun isReference(expr: Expr): Boolean = when (expr) {
            is ExprVarRef -> {
                val identifier = expr.identifier
                val root = identifier.qualifier.firstOrNull() ?: identifier.identifier
                matches(root)
            }
            is ExprPath -> isReference(expr.root)
            else -> false
        }

        // Consistent with the typer: a regular (unquoted) reference matches the element name case-insensitively.
        private fun matches(identifier: Identifier.Simple): Boolean =
            identifier.text.equals(name.text, ignoreCase = identifier.isRegular)
    }
}

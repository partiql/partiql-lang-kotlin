/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.planner.internal.transforms

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprIsType
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprStruct
import org.partiql.ast.Ast.exprStructField
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.queryBodySetOp
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectValue
import org.partiql.ast.AstRewriter
import org.partiql.ast.DataType
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromJoin
import org.partiql.ast.FromTableRef
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Literal.string
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.With
import org.partiql.ast.WithListElement
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCase
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprVarRef
import org.partiql.planner.internal.util.BinderUtils.toBinder

/**
 * Converts SQL-style SELECT to PartiQL SELECT VALUE.
 * - If there is a PROJECT ALL, we use the TUPLEUNION.
 * - If there is NOT a PROJECT ALL, we use a literal struct.
 *
 * Here are some example of rewrites:
 *
 * ```
 * SELECT *
 * FROM
 *   A AS x,
 *   B AS y AT i
 * ```
 * gets rewritten to:
 * ```
 * SELECT VALUE TUPLEUNION(
 *   CASE WHEN x IS STRUCT THEN x ELSE { '_1': x },
 *   CASE WHEN y IS STRUCT THEN y ELSE { '_2': y },
 *   { 'i': i }
 * ) FROM A AS x, B AS y AT i
 * ```
 *
 * ```
 * SELECT x.*, x.a FROM A AS x
 * ```
 * gets rewritten to:
 * ```
 * SELECT VALUE TUPLEUNION(
 *   CASE WHEN x IS STRUCT THEN x ELSE { '_1': x },
 *   { 'a': x.a }
 * ) FROM A AS x
 * ```
 *
 * ```
 * SELECT x.a FROM A AS x
 * ```
 * gets rewritten to:
 * ```
 * SELECT VALUE {
 *   'a': x.a
 * } FROM A AS x
 * ```
 *
 * NOTE: This does NOT transform subqueries. It operates directly on an [ExprQuerySet] -- and that is it. Therefore:
 * ```
 * SELECT
 *   (SELECT 1 FROM T AS "T")
 * FROM R AS "R"
 * ```
 * will be transformed to:
 * ```
 * SELECT VALUE {
 *   '_1': (SELECT 1 FROM T AS "T") -- notice that SELECT 1 didn't get transformed.
 * } FROM R AS "R"
 * ```
 *
 * Requires [NormalizeFromSource].
 */
internal object NormalizeSelect {

    internal fun normalize(node: ExprQuerySet): ExprQuerySet {
        val with = node.with?.elements?.map { element ->
            val elementQuery = normalize(element.asQuery)
            WithListElement(element.queryName, elementQuery, element.columnList)
        }?.let { With(it) }

        return when (val body = node.body) {
            is QueryBody.SFW -> {
                val sfw = Visitor.visitSFW(body, newCtx())
                exprQuerySet(
                    body = sfw,
                    orderBy = node.orderBy,
                    limit = node.limit,
                    offset = node.offset,
                    with = with
                )
            }
            is QueryBody.SetOp -> {
                val lhs = body.lhs.normalizeOrIdentity()
                val rhs = body.rhs.normalizeOrIdentity()
                exprQuerySet(
                    body = queryBodySetOp(
                        type = body.type,
                        isOuter = body.isOuter,
                        lhs = lhs,
                        rhs = rhs
                    ),
                    orderBy = node.orderBy,
                    limit = node.limit,
                    offset = node.offset,
                    with = with
                )
            }
            else -> error("Unexpected QueryBody type: $body")
        }
    }

    private fun Expr.normalizeOrIdentity(): Expr {
        return when (this) {
            is ExprQuerySet -> normalize(this)
            else -> this
        }
    }

    /**
     * Closure for incrementing a derived binding counter
     */
    private fun newCtx(): () -> Int = run {
        var i = 1;
        { i++ }
    }

    /**
     * The type parameter () -> Int
     */
    private object Visitor : AstRewriter<() -> Int>() {

        /**
         * This is used to give projections a name. For example:
         * ```
         * SELECT t.* FROM t AS t
         * ```
         *
         * Will get converted into:
         * ```
         * SELECT VALUE TUPLEUNION(
         *   CASE
         *     WHEN t IS STRUCT THEN t
         *     ELSE { '_1': t }
         *   END
         * )
         * FROM t AS t
         * ```
         *
         * In order to produce the struct's key in `{ '_1': t }` above, we use [col] to produce the column name
         * given the ordinal.
         */
        private val col = { index: Int -> "_${index + 1}" }

        fun visitSFW(node: QueryBody.SFW, ctx: () -> Int): QueryBody.SFW {
            val sfw = super.visitQueryBodySFW(node, ctx) as QueryBody.SFW
            return when (val select = sfw.select) {
                is SelectStar -> {
                    val selectValue = when (val group = sfw.groupBy) {
                        null -> visitSelectAll(select, sfw.from)
                        else -> visitSelectAll(select, group)
                    }
                    queryBodySFW(
                        select = selectValue,
                        exclude = sfw.exclude,
                        from = sfw.from,
                        let = sfw.let,
                        where = sfw.where,
                        groupBy = sfw.groupBy,
                        having = sfw.having,
                    )
                }
                else -> sfw
            }
        }

        override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: () -> Int): QueryBody.SFW {
            return node
        }

        override fun visitSelectList(node: SelectList, ctx: () -> Int): SelectValue {

            // Visit items, adding a binder if necessary
            var diff = false
            val visitedItems = ArrayList<SelectItem>(node.items.size)
            node.items.forEach { n ->
                val item = n.accept(this, ctx) as SelectItem
                if (item !== n) diff = true
                visitedItems.add(item)
            }
            val visitedNode = if (diff) selectList(visitedItems, node.setq) else node

            // Rewrite selection
            return when (node.items.any { it is SelectItem.Star }) {
                false -> visitSelectProjectWithoutProjectAll(visitedNode)
                true -> visitSelectProjectWithProjectAll(visitedNode)
            }
        }

        override fun visitSelectItemExpr(node: SelectItem.Expr, ctx: () -> Int): SelectItem.Expr {
            val expr = visitExpr(node.expr, newCtx()) as Expr
            val alias = when (node.asAlias) {
                null -> expr.toBinder(ctx)
                else -> node.asAlias
            }
            return if (expr != node.expr || alias != node.asAlias) {
                selectItemExpr(expr, alias)
            } else {
                node
            }
        }

        // Helpers

        /**
         * We need to call this from [visitQueryBodySFW] and not override [visitSelectStar] because we need access to the
         * [From] aliases.
         *
         * Note: We assume that [select] and [from] have already been visited.
         */
        private fun visitSelectAll(select: SelectStar, from: From): SelectValue {
            val tupleUnionArgs = from.tableRefs.flatMap { it.aliases() }.flatMapIndexed { i, binding ->
                val asAlias = binding.first
                val atAlias = binding.second
                val atAliasItem = atAlias?.simple()?.let {
                    val alias = it.asAlias ?: error("The AT alias should be present. This wasn't normalized.")
                    buildSimpleStruct(it.expr, alias.text)
                }
                listOfNotNull(
                    buildCaseWhenStruct(asAlias.star(i).expr, i),
                    atAliasItem,
                )
            }
            return selectValue(
                constructor = exprCall(
                    function = Identifier.delimited("TUPLEUNION"),
                    args = tupleUnionArgs,
                    setq = null // setq = null for scalar fn
                ),
                setq = select.setq
            )
        }

        /**
         * We need to call this from [visitQueryBodySFW] and not override [visitSelectStar] because we need access to the
         * [GroupBy] aliases.
         *
         * Note: We assume that [select] and [group] have already been visited.
         */
        private fun visitSelectAll(select: SelectStar, group: GroupBy): SelectValue {
            val groupAs = group.asAlias?.let { structField(it.text, varLocal(it.text)) }
            val fields = group.keys.map { key ->
                val alias = key.asAlias ?: error("Expected a GROUP BY alias.")
                structField(alias.text, varLocal(alias.text))
            } + listOfNotNull(groupAs)
            val constructor = exprStruct(fields)
            return selectValue(
                constructor = constructor,
                setq = select.setq
            )
        }

        private fun visitSelectProjectWithProjectAll(node: SelectList): SelectValue {
            val tupleUnionArgs = node.items.mapIndexed { index, item ->
                when (item) {
                    is SelectItem.Star -> buildCaseWhenStruct(item.expr, index)
                    is SelectItem.Expr -> buildSimpleStruct(
                        item.expr,
                        item.asAlias?.text
                            ?: error("The alias should've been here. This AST is not normalized.")
                    )
                    else -> error("Unexpected SelectItem type: $item")
                }
            }
            return selectValue(
                setq = node.setq,
                constructor = exprCall(
                    function = Identifier.regular("TUPLEUNION"),
                    args = tupleUnionArgs,
                    setq = null // setq = null for scalar fn
                )
            )
        }

        private fun visitSelectProjectWithoutProjectAll(node: SelectList): SelectValue {
            val structFields = node.items.map { item ->
                val itemExpr = item as? SelectItem.Expr ?: error("Expected the projection to be an expression.")
                exprStructField(
                    name = exprLit(string(itemExpr.asAlias?.text!!)),
                    value = item.expr
                )
            }
            return selectValue(
                setq = node.setq,
                constructor = exprStruct(
                    fields = structFields
                )
            )
        }

        private fun buildCaseWhenStruct(expr: Expr, index: Int): ExprCase = exprCase(
            expr = null,
            branches = listOf(
                exprCaseBranch(
                    condition = exprIsType(expr, DataType.STRUCT(), not = false),
                    expr = expr
                )
            ),
            defaultExpr = buildSimpleStruct(expr, col(index))
        )

        private fun buildSimpleStruct(expr: Expr, name: String): ExprStruct = exprStruct(
            fields = listOf(
                exprStructField(
                    name = exprLit(string(name)),
                    value = expr
                )
            )
        )

        private fun structField(name: String, expr: Expr): ExprStruct.Field = exprStructField(
            name = exprLit(string(name)),
            value = expr
        )

        private fun varLocal(name: String): ExprVarRef = exprVarRef(
            identifier = Identifier.delimited(name),
            isQualified = true
        )

        private fun FromTableRef.aliases(): List<Pair<String, String?>> = when (this) {
            is FromJoin -> lhs.aliases() + rhs.aliases()
            is FromExpr -> {
                val asAlias = asAlias?.text ?: error("AST not normalized, missing asAlias on FROM source.")
                val atAlias = atAlias?.text
                listOf(Pair(asAlias, atAlias))
            }
            else -> error("Unexpected FromTableRef type: $this")
        }

        // t -> t.* AS _i
        private fun String.star(i: Int): SelectItem.Expr {
            val expr = exprVarRef(Identifier.of(id(this)), isQualified = false)
            val alias = expr.toBinder(i)
            return selectItemExpr(expr, alias)
        }

        // t -> t AS t
        private fun String.simple(): SelectItem.Expr {
            val expr = exprVarRef(Identifier.of(id(this)), isQualified = false)
            val alias = id(this)
            return selectItemExpr(expr, alias)
        }

        private fun id(symbol: String) = Identifier.Simple.regular(symbol)
    }
}

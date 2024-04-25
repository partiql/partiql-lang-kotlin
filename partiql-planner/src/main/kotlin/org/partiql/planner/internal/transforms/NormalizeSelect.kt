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

import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.exprCall
import org.partiql.ast.exprCase
import org.partiql.ast.exprCaseBranch
import org.partiql.ast.exprIsType
import org.partiql.ast.exprLit
import org.partiql.ast.exprStruct
import org.partiql.ast.exprStructField
import org.partiql.ast.exprVar
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProject
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.selectValue
import org.partiql.ast.typeStruct
import org.partiql.ast.util.AstRewriter
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

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
 * NOTE: This does NOT transform subqueries. It operates directly on an [Expr.SFW] -- and that is it. Therefore:
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

    internal fun normalize(node: Expr.SFW): Expr.SFW {
        return Visitor.visitSFW(node, newCtx())
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

        internal fun visitSFW(node: Expr.SFW, ctx: () -> Int): Expr.SFW {
            val sfw = super.visitExprSFW(node, ctx) as Expr.SFW
            return when (val select = sfw.select) {
                is Select.Star -> {
                    val selectValue = when (val group = sfw.groupBy) {
                        null -> visitSelectAll(select, sfw.from)
                        else -> visitSelectAll(select, group)
                    }
                    sfw.copy(select = selectValue)
                }
                else -> sfw
            }
        }

        override fun visitExprSFW(node: Expr.SFW, ctx: () -> Int): Expr.SFW {
            return node
        }

        override fun visitSelectProject(node: Select.Project, ctx: () -> Int): Select.Value {

            // Visit items, adding a binder if necessary
            var diff = false
            val visitedItems = ArrayList<Select.Project.Item>(node.items.size)
            node.items.forEach { n ->
                val item = visitSelectProjectItem(n, ctx) as Select.Project.Item
                if (item !== n) diff = true
                visitedItems.add(item)
            }
            val visitedNode = if (diff) selectProject(visitedItems, node.setq) else node

            // Rewrite selection
            return when (node.items.any { it is Select.Project.Item.All }) {
                false -> visitSelectProjectWithoutProjectAll(visitedNode)
                true -> visitSelectProjectWithProjectAll(visitedNode)
            }
        }

        override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, ctx: () -> Int): Select.Project.Item.Expression {
            val expr = visitExpr(node.expr, newCtx()) as Expr
            val alias = when (node.asAlias) {
                null -> expr.toBinder(ctx)
                else -> node.asAlias
            }
            return if (expr != node.expr || alias != node.asAlias) {
                selectProjectItemExpression(expr, alias)
            } else {
                node
            }
        }

        // Helpers

        /**
         * We need to call this from [visitExprSFW] and not override [visitSelectStar] because we need access to the
         * [From] aliases.
         *
         * Note: We assume that [select] and [from] have already been visited.
         */
        private fun visitSelectAll(select: Select.Star, from: From): Select.Value {
            val tupleUnionArgs = from.aliases().flatMapIndexed { i, binding ->
                val asAlias = binding.first
                val atAlias = binding.second
                val byAlias = binding.third
                val atAliasItem = atAlias?.simple()?.let {
                    val alias = it.asAlias ?: error("The AT alias should be present. This wasn't normalized.")
                    buildSimpleStruct(it.expr, alias.symbol)
                }
                val byAliasItem = byAlias?.simple()?.let {
                    val alias = it.asAlias ?: error("The BY alias should be present. This wasn't normalized.")
                    buildSimpleStruct(it.expr, alias.symbol)
                }
                listOfNotNull(
                    buildCaseWhenStruct(asAlias.star(i).expr, i),
                    atAliasItem,
                    byAliasItem
                )
            }
            return selectValue(
                constructor = exprCall(
                    function = identifierSymbol("TUPLEUNION", Identifier.CaseSensitivity.SENSITIVE),
                    args = tupleUnionArgs
                ),
                setq = select.setq
            )
        }

        /**
         * We need to call this from [visitExprSFW] and not override [visitSelectStar] because we need access to the
         * [GroupBy] aliases.
         *
         * Note: We assume that [select] and [group] have already been visited.
         */
        private fun visitSelectAll(select: Select.Star, group: GroupBy): Select.Value {
            val groupAs = group.asAlias?.let { structField(it.symbol, varLocal(it.symbol)) }
            val fields = group.keys.map { key ->
                val alias = key.asAlias ?: error("Expected a GROUP BY alias.")
                structField(alias.symbol, varLocal(alias.symbol))
            } + listOfNotNull(groupAs)
            val constructor = exprStruct(fields)
            return selectValue(
                constructor = constructor,
                setq = select.setq
            )
        }

        private fun visitSelectProjectWithProjectAll(node: Select.Project): Select.Value {
            val tupleUnionArgs = node.items.mapIndexed { index, item ->
                when (item) {
                    is Select.Project.Item.All -> buildCaseWhenStruct(item.expr, index)
                    is Select.Project.Item.Expression -> buildSimpleStruct(
                        item.expr,
                        item.asAlias?.symbol
                            ?: error("The alias should've been here. This AST is not normalized.")
                    )
                }
            }
            return selectValue(
                setq = node.setq,
                constructor = exprCall(
                    function = identifierSymbol("TUPLEUNION", Identifier.CaseSensitivity.SENSITIVE),
                    args = tupleUnionArgs
                )
            )
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun visitSelectProjectWithoutProjectAll(node: Select.Project): Select.Value {
            val structFields = node.items.map { item ->
                val itemExpr = item as? Select.Project.Item.Expression ?: error("Expected the projection to be an expression.")
                exprStructField(
                    name = exprLit(stringValue(itemExpr.asAlias?.symbol!!)),
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

        @OptIn(PartiQLValueExperimental::class)
        private fun buildCaseWhenStruct(expr: Expr, index: Int): Expr.Case = exprCase(
            expr = null,
            branches = listOf(
                exprCaseBranch(
                    condition = exprIsType(expr, typeStruct(emptyList()), null),
                    expr = expr
                )
            ),
            default = buildSimpleStruct(expr, col(index))
        )

        @OptIn(PartiQLValueExperimental::class)
        private fun buildSimpleStruct(expr: Expr, name: String): Expr.Struct = exprStruct(
            fields = listOf(
                exprStructField(
                    name = exprLit(stringValue(name)),
                    value = expr
                )
            )
        )

        @OptIn(PartiQLValueExperimental::class)
        private fun structField(name: String, expr: Expr): Expr.Struct.Field = Expr.Struct.Field(
            name = Expr.Lit(stringValue(name)),
            value = expr
        )

        private fun varLocal(name: String): Expr.Var = Expr.Var(
            identifier = Identifier.Symbol(name, Identifier.CaseSensitivity.SENSITIVE),
            scope = Expr.Var.Scope.LOCAL
        )

        private fun From.aliases(): List<Triple<String, String?, String?>> = when (this) {
            is From.Join -> lhs.aliases() + rhs.aliases()
            is From.Value -> {
                val asAlias = asAlias?.symbol ?: error("AST not normalized, missing asAlias on FROM source.")
                val atAlias = atAlias?.symbol
                val byAlias = byAlias?.symbol
                listOf(Triple(asAlias, atAlias, byAlias))
            }
        }

        // t -> t.* AS _i
        private fun String.star(i: Int): Select.Project.Item.Expression {
            val expr = exprVar(id(this), Expr.Var.Scope.DEFAULT)
            val alias = expr.toBinder(i)
            return selectProjectItemExpression(expr, alias)
        }

        // t -> t AS t
        private fun String.simple(): Select.Project.Item.Expression {
            val expr = exprVar(id(this), Expr.Var.Scope.DEFAULT)
            val alias = id(this)
            return selectProjectItemExpression(expr, alias)
        }

        private fun id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
    }
}

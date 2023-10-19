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

package org.partiql.ast.normalize

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Statement
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
 * TODO: GROUP BY
 * TODO: LET
 *
 * Requires [NormalizeFromSource].
 */
internal object NormalizeSelect : AstPass {

    override fun apply(statement: Statement): Statement = Visitor.visitStatement(statement, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

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

        override fun visitExprSFW(node: Expr.SFW, ctx: Int): Expr.SFW {
            val sfw = super.visitExprSFW(node, ctx) as Expr.SFW
            return when (val select = sfw.select) {
                is Select.Star -> sfw.copy(select = visitSelectAll(select, sfw.from))
                else -> sfw
            }
        }

        override fun visitSelectProject(node: Select.Project, ctx: Int): AstNode {
            val visitedNode = super.visitSelectProject(node, ctx) as? Select.Project
                ?: error("VisitSelectProject should have returned a Select.Project")
            return when (node.items.any { it is Select.Project.Item.All }) {
                false -> visitSelectProjectWithoutProjectAll(visitedNode)
                true -> visitSelectProjectWithProjectAll(visitedNode)
            }
        }

        override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, ctx: Int): Select.Project.Item.Expression {
            val expr = visitExpr(node.expr, 0) as Expr
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

        private fun visitSelectProjectWithProjectAll(node: Select.Project): AstNode {
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
        private fun visitSelectProjectWithoutProjectAll(node: Select.Project): AstNode {
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
                    condition = exprIsType(expr, typeStruct(), null),
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

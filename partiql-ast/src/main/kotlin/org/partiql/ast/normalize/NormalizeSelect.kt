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
import org.partiql.ast.AstPass
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Statement
import org.partiql.ast.builder.AstBuilder
import org.partiql.ast.builder.ast
import org.partiql.ast.helpers.toBinder
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

        override fun visitExprSFW(node: Expr.SFW, ctx: Int) = ast {
            val sfw = super.visitExprSFW(node, ctx) as Expr.SFW
            when (val select = sfw.select) {
                is Select.Star -> sfw.copy(select = visitSelectAll(select, sfw.from))
                else -> sfw
            }
        }

        override fun visitSelectProject(node: Select.Project, ctx: Int): AstNode = ast {
            val visitedNode = super.visitSelectProject(node, ctx) as? Select.Project
                ?: error("VisitSelectProject should have returned a Select.Project")
            return@ast when (node.items.any { it is Select.Project.Item.All }) {
                false -> visitSelectProjectWithoutProjectAll(visitedNode)
                true -> visitSelectProjectWithProjectAll(visitedNode)
            }
        }

        override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, ctx: Int) = ast {
            val expr = visitExpr(node.expr, 0) as Expr
            val alias = when (node.asAlias) {
                null -> expr.toBinder(ctx)
                else -> node.asAlias
            }
            if (expr != node.expr || alias != node.asAlias) {
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
        private fun visitSelectAll(select: Select.Star, from: From): Select.Value = ast {
            val tupleUnionArgs = from.aliases().flatMapIndexed { i, binding ->
                val asAlias = binding.first
                val atAlias = binding.second
                val byAlias = binding.third
                val atAliasItem = atAlias?.simple()?.let {
                    val alias = it.asAlias ?: error("The AT alias should be present. This wasn't normalized.")
                    buildSimpleStruct(it.expr, alias.symbol)
                }
                val byAliasItem = byAlias?.simple()?.let {
                    val alias = it.asAlias ?: error("The AT alias should be present. This wasn't normalized.")
                    buildSimpleStruct(it.expr, alias.symbol)
                }
                listOfNotNull(
                    buildCaseWhenStruct(asAlias.star(i).expr, i),
                    atAliasItem,
                    byAliasItem
                )
            }
            selectValue {
                constructor = exprCall {
                    function = identifierSymbol("TUPLEUNION", Identifier.CaseSensitivity.SENSITIVE)
                    args.addAll(tupleUnionArgs)
                }
                setq = select.setq
            }
        }

        private fun visitSelectProjectWithProjectAll(node: Select.Project): AstNode = ast {
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
            selectValue {
                setq = node.setq
                constructor = exprCall {
                    function = identifierSymbol("TUPLEUNION", Identifier.CaseSensitivity.SENSITIVE)
                    args.addAll(tupleUnionArgs)
                }
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun visitSelectProjectWithoutProjectAll(node: Select.Project): AstNode = ast {
            val structFields = node.items.map { item ->
                val itemExpr = item as? Select.Project.Item.Expression ?: error("Expected the projection to be an expression.")
                exprStructField(
                    name = exprLit(stringValue(itemExpr.asAlias?.symbol!!)),
                    value = item.expr
                )
            }
            selectValue {
                setq = node.setq
                constructor = exprStruct {
                    fields.addAll(structFields)
                }
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun buildCaseWhenStruct(expr: Expr, index: Int): Expr.Case {
            return ast {
                exprCase {
                    branches.add(
                        exprCaseBranch(
                            condition = exprIsType(expr, typeStruct()),
                            expr = expr
                        )
                    )
                    default = buildSimpleStruct(expr, col(index))
                    exprStruct {
                        fields.add(
                            exprStructField(
                                name = exprLit(stringValue(index.toString())),
                                value = expr
                            )
                        )
                    }
                }
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun buildSimpleStruct(expr: Expr, name: String): Expr.Struct {
            return ast {
                exprStruct {
                    fields.add(
                        exprStructField(
                            name = exprLit(stringValue(name)),
                            value = expr
                        )
                    )
                }
            }
        }

        private fun From.aliases(): List<Triple<String, String?, String?>> = when (this) {
            is From.Join -> lhs.aliases() + rhs.aliases()
            is From.Value -> {
                val asAlias = asAlias?.symbol ?: error("AST not normalized, missing asAlias on FROM source.")
                val atAlias = atAlias?.symbol
                val byAlias = byAlias?.symbol
                listOf(Triple(asAlias, atAlias, byAlias))
            }
        }

        private val col = { index: Int -> "_${index + 1}" }

        // t -> t.* AS _i
        private fun String.star(i: Int) = ast {
            val expr = exprVar(id(this@star), Expr.Var.Scope.DEFAULT)
            val alias = expr.toBinder(i)
            selectProjectItemExpression(expr, alias)
        }

        // t -> t AS t
        private fun String.simple() = ast {
            val expr = exprVar(id(this@simple), Expr.Var.Scope.DEFAULT)
            val alias = id(this@simple)
            selectProjectItemExpression(expr, alias)
        }

        private fun AstBuilder.id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
    }
}

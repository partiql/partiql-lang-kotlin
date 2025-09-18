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

import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.OrderBy
import org.partiql.ast.SelectItem
import org.partiql.ast.Statement
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef

/**
 * Normalizes ORDER BY expressions by replacing SELECT aliases with their original expressions.
 * Maintains separate alias maps for each SFW clause layer to handle nested queries correctly.
 */
internal object OrderByAliasSupport : AstPass {
    override fun apply(statement: Statement): Statement {
        return Visitor.visitStatement(statement, ArrayDeque()) as Statement
    }

    private object Visitor : AstRewriter<ArrayDeque<MutableMap<String, Expr>>>() {

        override fun visitExprQuerySet(node: ExprQuerySet, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            ctx.addLast(mutableMapOf())
            val transformed = super.visitExprQuerySet(node, ctx)
            ctx.removeLast()
            return transformed
        }

        override fun visitSelectItem(node: SelectItem, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            if (node is SelectItem.Expr && node.asAlias != null) {
                ctx.last().put(node.asAlias!!.text, node.expr)
            }
            return super.visitSelectItem(node, ctx) ?: node
        }

        override fun visitOrderBy(node: OrderBy, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            val aliasMap = ctx.last()
            if (aliasMap.isEmpty()) return node

            val transformedSorts = node.sorts.map { sort ->
                val transformedExpr = resolveExpr(sort.expr, aliasMap)
                if (transformedExpr != sort.expr) {
                    sort(
                        expr = transformedExpr,
                        order = sort.order,
                        nulls = sort.nulls
                    )
                } else {
                    sort
                }
            }
            return orderBy(transformedSorts)
        }

        private fun resolveExpr(expr: Expr, aliasMap: MutableMap<String, Expr>): Expr {
            return when (expr) {
                is ExprVarRef -> {
                    val aliasName = expr.identifier.identifier.text
                    aliasMap[aliasName] ?: expr
                }
                else -> expr
            }
        }
    }
}

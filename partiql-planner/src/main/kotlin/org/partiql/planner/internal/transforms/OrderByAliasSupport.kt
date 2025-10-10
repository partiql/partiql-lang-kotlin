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

import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.sort
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.Statement
import org.partiql.ast.With
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.planner.internal.PErrors
import org.partiql.spi.errors.PErrorListener
import kotlin.collections.MutableMap

/**
 * Replaces ORDER BY aliases with their corresponding SELECT expressions using stack-based scope tracking.
 */
internal class OrderByAliasSupport(val listener: PErrorListener) : AstPass {
    override fun apply(statement: Statement): Statement {
        return Visitor(listener).visitStatement(statement, Context()) as Statement
    }

    /**
     * Context for tracking alias scopes across nested queries using a stack-based approach.
     *
     * Structure breakdown:
     * - **Stack (ArrayDeque)**: Each query level gets its own scope pushed/popped from stack
     * - **Map (String -> MutableList<Expr>)**: Maps alias names to their expressions within a scope
     * - **List (MutableList<Expr>)**: Handles duplicate aliases (same name, multiple expressions)
     *
     * Example for nested query with duplicates:
     * ```sql
     * SELECT pid AS p FROM (
     *   SELECT name AS x, age AS x FROM products ORDER BY x
     * ) ORDER BY p
     * ```
     *
     * Stack operations:
     * 1. Push outer scope: {"p" -> [pid]}
     * 2. Push inner scope: {"x" -> [name, age]} // List handles duplicate "x" aliases
     * 3. Resolve ORDER BY x: detects ambiguity from list size > 1
     * 4. Pop inner scope
     * 5. Resolve ORDER BY p: finds single expression from list
     * 6. Pop outer scope
     */
    data class Context(val aliasMapStack: ArrayDeque<MutableMap<String, MutableList<Expr>>> = ArrayDeque())

    /**
     * Maintains alias scope stack for nested queries and resolves ORDER BY aliases.
     */
    private class Visitor(val listener: PErrorListener) : AstRewriter<Context>() {
        /**
         * Pushes new alias scope, processes query, then pops scope.
         */
        override fun visitExprQuerySet(node: ExprQuerySet, ctx: Context): AstNode {
            // Push new scope
            ctx.aliasMapStack.addLast(mutableMapOf())

            // Visit all statements that may have SELECT or ORDER BY
            val body = node.body.let { visitQueryBody(it, ctx) as QueryBody }
            val orderBy = node.orderBy?.let {
                if (body is QueryBody.SetOp) {
                    // Skip alias replacement if the query body is set operations
                    node.orderBy
                } else {
                    visitOrderBy(it, ctx) as OrderBy?
                }
            }
            val with = node.with?.let { visitWith(it, ctx) as With? }
            val transformed = if (body !== node.body || orderBy !== node.orderBy || with !== node.with
            ) {
                exprQuerySet(body, orderBy, node.limit, node.offset, with)
            } else {
                node
            }

            // Pop scope
            ctx.aliasMapStack.removeLast()
            return transformed
        }

        /**
         * Collects SELECT aliases into current scope map.
         */
        override fun visitSelectItem(node: SelectItem, ctx: Context): AstNode {
            if (node is SelectItem.Expr) {
                node.asAlias?.let { alias ->
                    ctx.aliasMapStack.last().getOrPut(alias.text) { mutableListOf() }.add(node.expr)
                }
            }

            return node
        }

        /**
         * Replaces ORDER BY aliases with their SELECT expressions.
         */
        override fun visitOrderBy(node: OrderBy, ctx: Context): AstNode {
            val aliasMap = ctx.aliasMapStack.last()
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

        /**
         * Resolves variable references to their aliased expressions.
         * Regular identifiers use case-insensitive matching, delimited use case-sensitive.
         */
        private fun resolveExpr(expr: Expr, aliasMap: Map<String, List<Expr>>): Expr {
            return when (expr) {
                is ExprVarRef -> {
                    val identifier = expr.identifier.identifier
                    val orderByName = identifier.text

                    val candidates = if (identifier.isRegular) {
                        // This is O(N) look up time for each alias used in order by. In the case of performance concern,
                        // we need to consider implementing a normalized case-insensitive map for O(1) look up.
                        aliasMap.filterKeys { it.equals(orderByName, ignoreCase = true) }.values.flatten()
                    } else {
                        aliasMap[orderByName]
                    }

                    if (candidates == null) {
                        expr
                    } else if (candidates.size == 1) {
                        candidates[0]
                    } else {
                        if (candidates.size > 1) {
                            val candidateNames = candidates.mapNotNull {
                                val ref = it
                                if (ref is ExprVarRef) {
                                    ref.identifier.identifier.text
                                } else {
                                    null
                                }
                            }
                            listener.report(PErrors.varRefAmbiguous(null, expr.identifier, candidateNames))
                        }
                        expr
                    }
                }
                else -> expr
            }
        }
    }
}

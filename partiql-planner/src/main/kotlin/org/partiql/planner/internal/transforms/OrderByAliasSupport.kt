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
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.Statement
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.planner.internal.PErrors
import org.partiql.spi.errors.PErrorListener
import kotlin.collections.MutableMap

/**
 * Normalizes ORDER BY expressions by replacing SELECT aliases with their original expressions.
 * Uses a stack-based approach to maintain separate alias maps for each query scope,
 * enabling proper alias resolution in nested queries and set operations.
 */
internal class OrderByAliasSupport(val listener: PErrorListener) : AstPass {
    /**
     * Context for tracking parent query scopes and their alias mappings.
     *
     * @property parentStack Stack of ExprQuerySet nodes representing nested query scopes
     * @property aliasList Maps each query scope to its SELECT alias definitions
     */
    data class Context(val parentStack: ArrayDeque<ExprQuerySet> = ArrayDeque(), val aliasList: MutableMap<ExprQuerySet, MutableList<Pair<String, Expr>>> = mutableMapOf())

    override fun apply(statement: Statement): Statement {
        return Visitor(listener).visitStatement(statement, Context()) as Statement
    }

    /**
     * AST visitor that uses a stack-based approach to track parent query scopes.
     *
     * Key behaviors:
     * - Each ExprQuerySet creates its own alias scope on the stack
     * - SELECT aliases are collected into the current scope's map
     * - ORDER BY expressions resolve aliases from the appropriate scope
     * - Set operations (UNION, INTERSECT, EXCEPT) are skipped Order-By alias replacement
     * - Case sensitivity is handled for both regular and delimited identifiers
     *
     * Example with nested queries:
     * ```sql
     * SELECT pid AS p FROM (
     *   SELECT productId AS pid FROM products ORDER BY pid
     * ) ORDER BY p
     * ```
     *
     * Stack operations:
     * 1. Push outer query scope, collect "p" -> pid
     * 2. Push inner query scope, collect "pid" -> productId
     * 3. Resolve ORDER BY pid using inner scope
     * 4. Pop inner scope
     * 5. Resolve ORDER BY p using outer scope
     * 6. Pop outer scope
     */
    private class Visitor(val listener: PErrorListener) : AstRewriter<Context>() {
        /**
         * Manages query scope stack for each ExprQuerySet.
         * Pushes current query to stack on entry, pops on exit to maintain proper nesting.
         */
        override fun visitExprQuerySet(node: ExprQuerySet, ctx: Context): AstNode {
            // Push current query scope onto stack
            ctx.parentStack.addLast(node)
            ctx.aliasList[node] = mutableListOf()

            val transformed = super.visitExprQuerySet(node, ctx)

            // Pop scope when exiting query
            ctx.parentStack.removeLast()
            return transformed
        }

        /**
         * Collects SELECT aliases into the current query scope's alias map.
         * Only processes SelectItem.Expr nodes that have AS aliases defined.
         */
        override fun visitSelectItem(node: SelectItem, ctx: Context): AstNode {
            if (node is SelectItem.Expr) {
                node.asAlias?.let { alias ->
                    // Add alias mapping to current query scope
                    ctx.aliasList[ctx.parentStack.last()]?.add(alias.text to node.expr)
                }
            }
            return node
        }

        /**
         * Resolves ORDER BY expressions by replacing aliases with their original expressions.
         * For set operations, skip alias resolvation
         */
        override fun visitOrderBy(node: OrderBy, ctx: Context): AstNode {
            val parent = ctx.parentStack.last()
            // Skip alias replacement if OrderBy belongs to set operator.
            if (parent.body is QueryBody.SetOp) {
                return node
            }

            // Regular queries use their own alias map
            val aliasMap = ctx.aliasList[parent]!!
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
         * Resolves expressions recursively, handling aliases and complex expressions.
         *
         * Case sensitivity rules:
         * - Regular identifiers (unquoted): case-insensitive matching
         * - Delimited identifiers (quoted): case-sensitive matching
         *
         * @param expr Expression to resolve
         * @param aliasMap Current scope's alias mappings
         * @return Resolved expression or original if no alias found
         */
        private fun resolveExpr(expr: Expr, aliasMap: List<Pair<String, Expr>>): Expr {
            return when (expr) {
                is ExprVarRef -> {
                    val identifier = expr.identifier.identifier
                    val orderByName = identifier.text
                    val isOrderByRegular = identifier.isRegular

                    // Find matching alias considering case sensitivity
                    val candidates = aliasMap.filter { orderByName.equals(it.first, ignoreCase = isOrderByRegular) }

                    if (candidates.size == 1) {
                        candidates[0].second
                    } else {
                        if (candidates.size > 1) {
                            val candidateNames = candidates.map {
                                val ref = it.second
                                if (ref is ExprVarRef) {
                                    ref.identifier.identifier.text
                                } else {
                                    "Not a column name or alias"
                                }
                            }.toList()
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

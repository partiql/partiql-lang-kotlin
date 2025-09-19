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

    /**
     * AST visitor that maintains a stack of alias maps for nested query scopes.
     *
     * ArrayDeque<MutableMap<String, Expr>> is used to handle nested SFW statements where:
     * - Each query level has its own alias scope
     * - Inner queries can't reference outer query aliases in ORDER BY
     * - The stack ensures proper scoping isolation
     *
     * Example nested SFW query:
     * ```sql
     * SELECT pid AS p FROM (
     *   SELECT productId AS pid, price AS cost FROM products ORDER BY cost
     * ) AS outer ORDER BY p
     * ```
     *
     * Function call sequence for the above query:
     * 1. visitExprQuerySet (outer) - pushes map1 to stack: [map1]
     * 2. visitSelectItem (pid AS p) - adds "p" -> pid to map1
     * 3. inside From, visitExprQuerySet (inner) - pushes map2 to stack: [map1, map2]
     * 4. visitSelectItem (productId AS pid) - adds "pid" -> productId to map2
     * 5. visitSelectItem (price AS cost) - adds "cost" -> price to map2
     * 6. visitOrderBy (ORDER BY cost) - resolves "cost" using map2 (current scope)
     * 7. visitExprQuerySet (inner) exits - pops map2: [map1]
     * 8. visitOrderBy (ORDER BY p) - resolves "p" using map1 (outer scope)
     * 9. visitExprQuerySet (outer) exits - pops map1: []
     */
    private object Visitor : AstRewriter<ArrayDeque<MutableMap<String, Expr>>>() {

        /**
         * Creates a new alias scope for each query level.
         * Pushes a new map to the stack on entry, pops on exit.
         */
        override fun visitExprQuerySet(node: ExprQuerySet, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            ctx.addLast(mutableMapOf())
            val transformed = super.visitExprQuerySet(node, ctx)
            ctx.removeLast()
            return transformed
        }

        /**
         * Collects aliases from SELECT items into the current scope's map.
         * Only SELECT items with AS aliases are stored.
         */
        override fun visitSelectItem(node: SelectItem, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            if (node is SelectItem.Expr && node.asAlias != null) {
                ctx.last().put(node.asAlias!!.text, node.expr)
            }
            return super.visitSelectItem(node, ctx) ?: node
        }

        /**
         * Resolves ORDER BY expressions using the current scope's alias map.
         * Replaces alias references with their original expressions.
         */
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

        /**
         * Resolves variable references to their aliased expressions.
         * Returns the original expression if no alias mapping exists.
         */
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

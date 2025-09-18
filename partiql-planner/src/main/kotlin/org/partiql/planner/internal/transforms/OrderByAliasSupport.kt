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
import org.partiql.ast.GroupBy
import org.partiql.ast.OrderBy
import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectList
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.Statement
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprVarRef

/**
 * Normalizes ORDER BY expressions by replacing SELECT aliases with their original expressions.
 * Maintains separate alias maps for each SFW clause layer to handle nested queries correctly.
 */
internal object OrderByAliasSupport : AstPass {
    override fun apply(statement: Statement): Statement {
        return Visitor.visitStatement(statement, ArrayDeque()) as Statement
    }

    fun resolveAliases(expr: Expr, ctx: MutableMap<String, Expr>): Expr {
        return expr
    }

    private object Visitor : AstRewriter<ArrayDeque<MutableMap<String, Expr>>>() {
        override fun visitQuery(node: Query, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            ctx.addLast(mutableMapOf())
            super.visitQuery(node, ctx)
            ctx.removeLast()

            return node
        }

        override fun visitSelectList(node: SelectList, ctx: ArrayDeque<MutableMap<String, Expr>>): SelectList {
            super.visitSelectList(node, ctx)


            return node
        }

        override fun visitGroupBy(node: GroupBy, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            super.visitGroupBy(node, ctx)
            return node
        }

        override fun visitOrderBy(node: OrderBy, ctx: ArrayDeque<MutableMap<String, Expr>>): AstNode {
            super.visitOrderBy(node, ctx)

            return node
        }
    }
}
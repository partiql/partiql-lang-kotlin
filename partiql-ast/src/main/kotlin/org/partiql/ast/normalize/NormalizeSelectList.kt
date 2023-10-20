/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.ast.normalize

import org.partiql.ast.Expr
import org.partiql.ast.Select
import org.partiql.ast.Statement
import org.partiql.ast.builder.ast
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter

/**
 * Adds an `as` alias to every select-list item.
 *
 * - [org.partiql.ast.helpers.toBinder]
 * - https://partiql.org/assets/PartiQL-Specification.pdf#page=28
 * - https://web.cecs.pdx.edu/~len/sql1999.pdf#page=287
 */
internal object NormalizeSelectList : AstPass {

    override fun apply(statement: Statement) = Visitor.visitStatement(statement, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

        override fun visitSelectProject(node: Select.Project, ctx: Int) = ast {
            if (node.items.isEmpty()) {
                return@ast node
            }
            var diff = false
            val transformed = ArrayList<Select.Project.Item>(node.items.size)
            node.items.forEachIndexed { i, n ->
                val item = visitSelectProjectItem(n, i) as Select.Project.Item
                if (item !== n) diff = true
                transformed.add(item)
            }
            // We don't want to create a new list unless we have to, as to not trigger further rewrites up the tree.
            if (diff) selectProject(transformed) else node
        }

        override fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: Int) = node.copy()

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
    }
}

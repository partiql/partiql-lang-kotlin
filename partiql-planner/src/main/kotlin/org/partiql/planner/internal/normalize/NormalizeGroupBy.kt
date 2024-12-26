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

package org.partiql.planner.internal.normalize

import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.GroupBy
import org.partiql.ast.Query
import org.partiql.ast.Statement
import org.partiql.ast.expr.Expr
import org.partiql.planner.internal.helpers.toBinder

/**
 * Adds a unique binder to each group key.
 */
internal object NormalizeGroupBy : AstPass {

    override fun apply(statement: Statement) = when (statement) {
        is Query -> Visitor.visitStatement(statement, 0) as Statement
        else -> statement
    }

    private object Visitor : AstRewriter<Int>() {

        override fun visitGroupBy(node: GroupBy, ctx: Int): AstNode {
            val keys = node.keys.mapIndexed { index, key ->
                visitGroupByKey(key, index + 1)
            }
            return groupBy(strategy = node.strategy, keys = keys, asAlias = node.asAlias)
        }

        override fun visitGroupByKey(node: GroupBy.Key, ctx: Int): GroupBy.Key {
            val expr = visitExpr(node.expr, 0) as Expr
            val alias = when (node.asAlias) {
                null -> expr.toBinder(ctx)
                else -> node.asAlias
            }
            return if (expr !== node.expr || alias !== node.asAlias) {
                groupByKey(expr, alias)
            } else {
                node
            }
        }
    }
}

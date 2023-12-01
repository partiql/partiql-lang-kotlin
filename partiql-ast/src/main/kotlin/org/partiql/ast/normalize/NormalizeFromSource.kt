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

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Statement
import org.partiql.ast.fromJoin
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter

/**
 * Assign aliases to any FROM source which does not have one.
 */
internal object NormalizeFromSource : AstPass {

    override fun apply(statement: Statement): Statement = Visitor.visitStatement(statement, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

        // Each SFW starts the ctx count again.
        override fun visitExprSFW(node: Expr.SFW, ctx: Int): AstNode = super.visitExprSFW(node, 0)

        override fun visitStatementDMLBatchLegacy(node: Statement.DML.BatchLegacy, ctx: Int): AstNode =
            super.visitStatementDMLBatchLegacy(node, 0)

        override fun visitFrom(node: From, ctx: Int) = super.visitFrom(node, ctx) as From

        override fun visitFromJoin(node: From.Join, ctx: Int): From {
            val lhs = visitFrom(node.lhs, ctx)
            val rhs = visitFrom(node.rhs, ctx + 1)
            val condition = node.condition?.let { visitExpr(it, ctx) as Expr }
            return if (lhs !== node.lhs || rhs !== node.rhs || condition !== node.condition) {
                fromJoin(lhs, rhs, node.type, condition)
            } else {
                node
            }
        }

        override fun visitFromValue(node: From.Value, ctx: Int): From {
            val expr = visitExpr(node.expr, ctx) as Expr
            val asAlias = node.asAlias ?: expr.toBinder(ctx)
            return if (expr !== node.expr || asAlias !== node.asAlias) {
                node.copy(expr = expr, asAlias = asAlias)
            } else {
                node
            }
        }
    }
}

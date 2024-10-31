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

import org.partiql.ast.v1.Ast.fromExpr
import org.partiql.ast.v1.Ast.fromJoin
import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstRewriter
import org.partiql.ast.v1.From
import org.partiql.ast.v1.FromExpr
import org.partiql.ast.v1.FromJoin
import org.partiql.ast.v1.FromTableRef
import org.partiql.ast.v1.FromType
import org.partiql.ast.v1.QueryBody
import org.partiql.ast.v1.Statement
import org.partiql.ast.v1.expr.Expr
import org.partiql.planner.internal.helpers.toBinder

/**
 * Assign aliases to any FROM source which does not have one.
 */
internal object NormalizeFromSource : AstPass {

    override fun apply(statement: Statement): Statement = statement.accept(Visitor, 0) as Statement

    private object Visitor : AstRewriter<Int>() {

        // Each SFW starts the ctx count again.
        override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: Int): AstNode = super.visitQueryBodySFW(node, 0)

        override fun visitFrom(node: From, ctx: Int) = super.visitFrom(node, ctx) as From

        override fun visitFromJoin(node: FromJoin, ctx: Int): FromJoin {
            val lhs = node.lhs.accept(this, ctx) as FromTableRef
            val rhs = node.rhs.accept(this, ctx + 1) as FromTableRef
            val condition = node.condition?.accept(this, ctx) as Expr?
            return if (lhs !== node.lhs || rhs !== node.rhs || condition !== node.condition) {
                fromJoin(lhs, rhs, node.joinType, condition)
            } else {
                node
            }
        }

        override fun visitFromExpr(node: FromExpr, ctx: Int): FromExpr {
            val expr = node.expr.accept(this, ctx) as Expr
            var i = ctx
            var asAlias = node.asAlias
            var atAlias = node.atAlias
            // derive AS alias
            if (asAlias == null) {
                asAlias = expr.toBinder(i++)
            }
            // derive AT binder
            if (atAlias == null && node.fromType == FromType.UNPIVOT()) {
                atAlias = expr.toBinder(i++)
            }
            return if (expr !== node.expr || asAlias !== node.asAlias || atAlias !== node.atAlias) {
                fromExpr(expr = expr, fromType = node.fromType, asAlias = asAlias, atAlias = atAlias)
            } else {
                node
            }
        }
    }
}

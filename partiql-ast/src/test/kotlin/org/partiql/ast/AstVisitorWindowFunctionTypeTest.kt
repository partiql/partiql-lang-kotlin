/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
package org.partiql.ast

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.sort
import org.partiql.ast.Ast.windowSpecification

/**
 * Regression for [AstVisitor] leaf visits on rank-style [WindowFunctionType] nodes that previously
 * re-dispatched via [AstNode.accept] and overflowed the stack.
 */
class AstVisitorWindowFunctionTypeTest {

    @ParameterizedTest
    @MethodSource("rankStyleWindowFunctionTypes")
    fun defaultVisitorTraversesRankStyleWindowFunctionsWithoutStackOverflow(type: WindowFunctionType) {
        val query = selectWithWindowFunction(type)
        val visitor = object : AstVisitor<Unit, Unit>() {
            override fun defaultReturn(node: AstNode, ctx: Unit) = Unit
        }
        assertDoesNotThrow { query.accept(visitor, Unit) }
    }

    private fun selectWithWindowFunction(type: WindowFunctionType): AstNode {
        val orderByT_a = orderBy(
            listOf(
                sort(
                    exprPath(
                        root = exprVarRef(Identifier.regular("t"), isQualified = false),
                        steps = listOf(exprPathStepField(Identifier.Simple.regular("a"))),
                    ),
                    Order.ASC(),
                    Nulls.LAST(),
                ),
            ),
        )
        val spec = windowSpecification(null, emptyList(), orderByT_a)
        return exprQuerySet(
            queryBodySFW(
                select = selectList(
                    items = listOf(selectItemExpr(exprWindowFunction(type, spec))),
                    setq = null,
                ),
                from = from(
                    listOf(
                        fromExpr(
                            expr = exprVarRef(Identifier.regular("t"), isQualified = false),
                            fromType = FromType.SCAN(),
                            asAlias = null,
                            atAlias = null,
                        ),
                    ),
                ),
            ),
        )
    }

    companion object {
        @JvmStatic
        fun rankStyleWindowFunctionTypes(): List<WindowFunctionType> = listOf(
            WindowFunctionType.RowNumber(),
            WindowFunctionType.Rank(),
            WindowFunctionType.DenseRank(),
            WindowFunctionType.PercentRank(),
            WindowFunctionType.CumeDist(),
        )
    }
}

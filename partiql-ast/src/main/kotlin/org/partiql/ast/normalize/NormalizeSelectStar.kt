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
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Statement
import org.partiql.ast.builder.AstBuilder
import org.partiql.ast.builder.ast
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter

/**
 * Rewrites
 *    - `SELECT * FROM A AS x, B AS y AT i` -> `SELECT x.* AS _1, y.* as _2, i AS i FROM A AS x, B AS y AT i`
 *    - TODO GROUP BY
 *
 * Requires [NormalizeFromSource]
 */
internal object NormalizeSelectStar : AstPass {

    override fun apply(statement: Statement): Statement = Visitor.visitStatement(statement, Unit) as Statement

    private object Visitor : AstRewriter<Unit>() {

        override fun visitExprSFW(node: Expr.SFW, ctx: Unit) = ast {
            val sfw = super.visitExprSFW(node, ctx) as Expr.SFW
            if (sfw.select !is Select.Star) {
                return@ast sfw
            }
            val sel = selectProject {
                sfw.from.aliases().forEachIndexed { i, binding ->
                    val asAlias = binding.first
                    val atAlias = binding.second
                    val byAlias = binding.third
                    items += asAlias.star(i)
                    if (atAlias != null) items += atAlias.simple()
                    if (byAlias != null) items += byAlias.simple()
                }
                setq = (sfw.select as Select.Star).setq
            }
            sfw.copy(select = sel)
        }

        // Helpers

        private fun From.aliases(): List<Triple<String, String?, String?>> = when (this) {
            is From.Join -> lhs.aliases() + rhs.aliases()
            is From.Value -> {
                val asAlias = asAlias?.symbol ?: error("AST not normalized, missing asAlias on FROM source.")
                val atAlias = atAlias?.symbol
                val byAlias = byAlias?.symbol
                listOf(Triple(asAlias, atAlias, byAlias))
            }
        }

        // t -> t.* AS _i
        private fun String.star(i: Int) = ast {
            val expr = exprPath {
                root = exprVar(id(this@star), Expr.Var.Scope.DEFAULT)
                steps += exprPathStepUnpivot()
            }
            val alias = expr.toBinder(i)
            selectProjectItemExpression(expr, alias)
        }

        // t -> t AS t
        private fun String.simple() = ast {
            val expr = exprVar(id(this@simple), Expr.Var.Scope.DEFAULT)
            val alias = id(this@simple)
            selectProjectItemExpression(expr, alias)
        }

        private fun AstBuilder.id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
    }
}

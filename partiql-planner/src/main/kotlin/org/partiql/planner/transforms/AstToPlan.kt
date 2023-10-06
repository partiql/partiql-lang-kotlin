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
 *
 */

package org.partiql.planner.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.identifierQualified
import org.partiql.plan.identifierSymbol
import org.partiql.plan.statementQuery
import org.partiql.planner.Env
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Identifier as PlanIdentifier
import org.partiql.plan.Statement as PlanStatement

/**
 * Simple translation from AST to an unresolved algebraic IR.
 */
internal object AstToPlan {

    // statement.toPlan()
    @JvmStatic
    fun apply(statement: AstStatement, env: Env): PlanStatement = statement.accept(ToPlanStatement, env)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToPlanStatement : AstBaseVisitor<PlanStatement, Env>() {

        override fun defaultReturn(node: AstNode, env: Env) = throw IllegalArgumentException("Unsupported statement")

        override fun visitStatementQuery(node: AstStatement.Query, env: Env): PlanStatement {
            val rex = when (val expr = node.expr) {
                is Expr.SFW -> RelConverter.apply(expr, env)
                else -> RexConverter.apply(expr, env)
            }
            return statementQuery(rex)
        }
    }

    // --- Helpers --------------------

    fun convert(identifier: AstIdentifier): PlanIdentifier = when (identifier) {
        is AstIdentifier.Qualified -> convert(identifier)
        is AstIdentifier.Symbol -> convert(identifier)
    }

    fun convert(identifier: AstIdentifier.Qualified): PlanIdentifier.Qualified {
        val root = convert(identifier.root)
        val steps = identifier.steps.map { convert(it) }
        return identifierQualified(root, steps)
    }

    fun convert(identifier: AstIdentifier.Symbol): PlanIdentifier.Symbol {
        val symbol = identifier.symbol
        val case = when (identifier.caseSensitivity) {
            AstIdentifier.CaseSensitivity.SENSITIVE -> PlanIdentifier.CaseSensitivity.SENSITIVE
            AstIdentifier.CaseSensitivity.INSENSITIVE -> PlanIdentifier.CaseSensitivity.INSENSITIVE
        }
        return identifierSymbol(symbol, case)
    }
}

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

package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.Statement as AstStatement
import org.partiql.planner.internal.ir.Statement as PlanStatement

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

    fun convert(identifier: AstIdentifier): Identifier = when (identifier) {
        is AstIdentifier.Qualified -> convert(identifier)
        is AstIdentifier.Symbol -> convert(identifier)
    }

    fun convert(identifier: AstIdentifier.Qualified): Identifier {
        val parts = mutableListOf<Identifier.Part>()
        parts.add(part(identifier.root))
        parts.addAll(identifier.steps.map { part(it) })
        return Identifier.of(parts)
    }

    fun convert(identifier: AstIdentifier.Symbol): Identifier {
        return Identifier.of(part(identifier))
    }

    fun part(identifier: AstIdentifier.Symbol): Identifier.Part = when (identifier.caseSensitivity) {
        AstIdentifier.CaseSensitivity.SENSITIVE -> Identifier.Part.delimited(identifier.symbol)
        AstIdentifier.CaseSensitivity.INSENSITIVE -> Identifier.Part.regular(identifier.symbol)
    }
}

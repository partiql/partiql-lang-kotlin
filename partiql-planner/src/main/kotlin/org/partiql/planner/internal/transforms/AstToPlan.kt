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

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.Query
import org.partiql.ast.v1.expr.ExprQuerySet
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.spi.catalog.Identifier
import org.partiql.ast.v1.Identifier as AstIdentifier
import org.partiql.ast.v1.IdentifierChain as AstIdentifierChain
import org.partiql.ast.v1.Statement as AstStatement
import org.partiql.planner.internal.ir.Statement as PlanStatement

/**
 * Simple translation from AST to an unresolved algebraic IR.
 */
internal object AstToPlan {

    // statement.toPlan()
    @JvmStatic
    fun apply(statement: AstStatement, env: Env): PlanStatement = statement.accept(ToPlanStatement, env)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToPlanStatement : AstVisitor<PlanStatement, Env>() {

        override fun defaultReturn(node: AstNode, env: Env) = throw IllegalArgumentException("Unsupported statement")

        override fun visitQuery(node: Query, env: Env): PlanStatement {
            val rex = when (val expr = node.expr) {
                is ExprQuerySet -> RelConverter.apply(expr, env)
                else -> RexConverter.apply(expr, env)
            }
            return statementQuery(rex)
        }
    }

    // --- Helpers --------------------

    fun convert(identifier: AstIdentifierChain): Identifier {
        val parts = mutableListOf<Identifier.Part>()
        parts.add(part(identifier.root))
        var curStep = identifier.next
        while (curStep != null) {
            parts.add(part(curStep.root))
            curStep = curStep.next
        }
        return Identifier.of(parts)
    }

    fun convert(identifier: AstIdentifier): Identifier {
        return Identifier.of(part(identifier))
    }

    fun part(identifier: AstIdentifier): Identifier.Part = when (identifier.isDelimited) {
        true -> Identifier.Part.delimited(identifier.symbol)
        false -> Identifier.Part.regular(identifier.symbol)
    }
}

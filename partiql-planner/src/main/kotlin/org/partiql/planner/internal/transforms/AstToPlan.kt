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
import org.partiql.ast.AstVisitor
import org.partiql.ast.Query
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.spi.catalog.Identifier
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

    fun convert(node: AstIdentifier): Identifier {
        val qualifier = node.qualifier.map { part(it) }
        val identifier = part(node.identifier)
        return Identifier.of(qualifier + identifier)
    }

    fun convert(identifier: org.partiql.ast.Identifier.Simple): Identifier {
        return Identifier.of(part(identifier))
    }

    fun part(identifier: org.partiql.ast.Identifier.Simple): Identifier.Simple = when (identifier.isRegular) {
        true -> Identifier.Simple.regular(identifier.text)
        false -> Identifier.Simple.delimited(identifier.text)
    }
}

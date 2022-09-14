/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * A [PartiqlAst.VisitorTransform] to replace the [PartiqlAst.SortSpec] of a [PartiqlAst.OrderBy] with a reference to
 * a [PartiqlAst.ProjectItem]'s [PartiqlAst.Expr] if an alias is provided.
 *
 * Turns:
 *
 * ```SELECT a + 1 AS b FROM c ORDER BY b```
 *
 * Into:
 *
 * ```SELECT a + 1 AS b FROM c ORDER BY a + 1```
 */
class OrderByAliasVisitorTransform : VisitorTransformBase() {

    private val projectionAliases: MutableMap<String, PartiqlAst.Expr> = mutableMapOf<String, PartiqlAst.Expr>()

    /**
     * Nests itself to ensure ORDER BYs don't have access to the same [projectionAliases]
     */
    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return OrderByAliasVisitorTransform().transformExprSelectEvaluationOrder(node)
    }

    /**
     * Uses default transform and adds the alias to the [projectionAliases] map
     */
    override fun transformProjectItemProjectExpr_asAlias(node: PartiqlAst.ProjectItem.ProjectExpr): SymbolPrimitive? {
        val transformedAlias = super.transformProjectItemProjectExpr_asAlias(node)
        if (node.asAlias != null) { projectionAliases[node.asAlias.text] = node.expr }
        return transformedAlias
    }

    /**
     * Uses the [OrderByAliasSupport] class to transform any encountered IDs in ORDER BY <sortSpec> into the appropriate
     * expression using the [projectionAliases]
     */
    override fun transformSortSpec_expr(node: PartiqlAst.SortSpec): PartiqlAst.Expr {
        return OrderByAliasSupport(projectionAliases).transformSortSpec_expr(node)
    }

    /**
     * A [PartiqlAst.VisitorTransform] that converts any found Expr.Id's into what it is mapped to in [aliases]
     */
    class OrderByAliasSupport(val aliases: Map<String, PartiqlAst.Expr>) : VisitorTransformBase() {
        override fun transformExprId(node: PartiqlAst.Expr.Id): PartiqlAst.Expr {
            val transformedExpr = super.transformExprId(node)
            return when (node.case) {
                is PartiqlAst.CaseSensitivity.CaseSensitive -> aliases[node.name.text] ?: transformedExpr
                else -> aliases[node.name.text.toLowerCase()] ?: aliases[node.name.text.toUpperCase()] ?: transformedExpr
            }
        }
    }
}

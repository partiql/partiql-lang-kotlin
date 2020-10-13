/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.AggregateCallSiteListMeta
import org.partiql.lang.ast.AggregateRegisterIdMeta
import org.partiql.lang.ast.SelectProjectionValue
import org.partiql.lang.ast.metaContainerOf
import org.partiql.lang.ast.toIonElementMetaContainer
import org.partiql.lang.domains.PartiqlAst

/**
 * Allocates registerIds to all aggregate call-sites, storing the allocated registerId in an instance of
 * [AggregateRegisterIdMeta].
 *
 * Also collects a list of all aggregate functions used in the `SELECT` list of the current query (excluding subqueries)
 * and stores them in an instance of [AggregateCallSiteListMeta] on the [PartiqlAst.Expr.Select] instance.
 */

class AggregateSupportVisitorTransform : PartiqlAst.VisitorTransform() {
    private val aggregateCallSites = ArrayList<PartiqlAst.Expr.CallAgg>()

    inner class RegisterIdAdderSubVisitorTransform : PartiqlAst.VisitorTransform() {
        /**
         * Nests another [AggregateSupportVisitorTransform] within this transform
         * in order to avoid operating on [aggregateCallSites] of a nested SELECT.
         */
        override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr =
            AggregateSupportVisitorTransform().transformExpr(node)

        override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
            aggregateCallSites.add(node)

            return PartiqlAst.build {
                callAgg_(
                    setq = node.setq,
                    funcName = node.funcName,
                    arg = transformExpr(node.arg),
                    metas = transformMetas(node.arg.metas) + metaContainerOf(AggregateRegisterIdMeta(aggregateCallSites.size - 1)).toIonElementMetaContainer()
                )
            }
        }
    }

    private val registerIdAdder = RegisterIdAdderSubVisitorTransform()

    /**
     * Applies the [registerIdAdder] only to expressions in select list items.
     */
    override fun transformProjectItemProjectExpr(node: PartiqlAst.ProjectItem.ProjectExpr): PartiqlAst.ProjectItem =
        PartiqlAst.build {
            projectExpr_(
                expr = registerIdAdder.transformExpr(node.expr),
                asAlias = node.asAlias,
                metas = node.metas
            )
        }

    /**
     * Applies a new instance of [AggregateSupportVisitorTransform]
     * to [SelectProjectionValue] nodes so that a new they different instance of [aggregateCallSites].
     */
    override fun transformProjectionProjectValue(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Projection =
        PartiqlAst.build {
            projectValue(
                value = AggregateSupportVisitorTransform().transformExpr(node.value),
                metas = node.metas
            )
    }

    override fun transformExprSelect_having(node: PartiqlAst.Expr.Select): PartiqlAst.Expr? =
        node.having?.let { registerIdAdder.transformExpr(it) }


    override fun transformExprSelect_metas(node: PartiqlAst.Expr.Select): MetaContainer =
        transformMetas(node.metas) + metaContainerOf(AggregateCallSiteListMeta(aggregateCallSites.toList())).toIonElementMetaContainer()

}

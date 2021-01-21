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
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.lang.ast.AggregateCallSiteListMeta
import org.partiql.lang.ast.AggregateRegisterIdMeta
import org.partiql.lang.domains.PartiqlAst

/**
 * Allocates registerIds to all aggregate call-sites, storing the allocated registerId in an instance of
 * [AggregateRegisterIdMeta].
 *
 * Also collects a list of all aggregate functions used in the `SELECT` of the current query (excluding subqueries)
 * and stores them in an instance of [AggregateCallSiteListMeta] on the [PartiqlAst.Expr.Select] instance.
 */

class AggregateSupportVisitorTransform : VisitorTransformBase() {
    private val aggregateCallSites = ArrayList<PartiqlAst.Expr.CallAgg>()

    /**
     * Nests another [AggregateSupportVisitorTransform] within this transform to avoid operating on
     * [aggregateCallSites] of a nested `SELECT`.
     */
    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return AggregateSupportVisitorTransform().transformExprSelectEvaluationOrder(node)
    }

    override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
        val transformedCallAgg = PartiqlAst.build {
            callAgg_(
                setq = node.setq,
                funcName = node.funcName,
                arg = transformExpr(node.arg),
                metas = transformMetas(node.metas) + metaContainerOf(AggregateRegisterIdMeta.TAG to AggregateRegisterIdMeta(aggregateCallSites.size)))
        }
        aggregateCallSites.add(transformedCallAgg)
        return transformedCallAgg
    }

    /**
     * Applies a new instance of [AggregateSupportVisitorTransform] to [PartiqlAst.Projection.ProjectValue] nodes so
     * that a different instance of [aggregateCallSites] is used.
     */
    override fun transformProjectionProjectValue_value(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Expr =
        AggregateSupportVisitorTransform().transformExpr(node.value)

    override fun transformExprSelect_metas(node: PartiqlAst.Expr.Select): MetaContainer =
        transformMetas(node.metas) + metaContainerOf(AggregateCallSiteListMeta.TAG to AggregateCallSiteListMeta(aggregateCallSites.toList()))
}

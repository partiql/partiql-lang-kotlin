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

package org.partiql.lang.ast.passes

import org.partiql.lang.ast.*

/**
 * Allocates registerIds to all aggregate call-sites, storing the allocated registerId in an instance of
 * [AggregateRegisterIdMeta].
 *
 * Also collects a list of all aggregate functions used in the `SELECT` list of the current query (excluding subqueries)
 * and stores them in an instance of [AggregateCallSiteListMeta] on the [Select] instance.
 */
class AggregateSupportRewriter : AstRewriterBase() {
    private val aggregateCallSites = ArrayList<CallAgg>()

    inner class RegisterIdAdderSubRewriter : AstRewriterBase() {
        /**
         * Nests another [AggregateSupportRewriter] within this rewrite
         * in order to avoid operating on [aggregateCallSites] of a nested SELECT.
         */
        override fun rewriteSelect(selectExpr: Select): ExprNode =
            AggregateSupportRewriter().rewriteExprNode(selectExpr)

        override fun rewriteCallAgg(node: CallAgg): ExprNode {
            aggregateCallSites.add(node)

            return CallAgg(
                funcExpr = rewriteExprNode(node.funcExpr),
                setQuantifier = node.setQuantifier,
                arg = rewriteExprNode(node.arg),
                metas = rewriteMetas(node.arg).add(AggregateRegisterIdMeta(aggregateCallSites.size - 1))
            )
        }
    }

    private val registerIdAdder = RegisterIdAdderSubRewriter()

    /**
     * Applies the [RegisterIdAdderSubRewriter] only to expressions in select list items.
     */
    override fun rewriteSelectListItemExpr(item: SelectListItemExpr): SelectListItem =
        SelectListItemExpr(
            expr = registerIdAdder.rewriteExprNode(item.expr),
            asName = item.asName)

    /**
     * Applies a new instance of [AggregateSupportRewriter]
     * to [SelectProjectionValue] nodes so that a new they different instance of [aggregateCallSites].
     */
    override fun rewriteSelectProjectionValue(projection: SelectProjectionValue): SelectProjection =
        SelectProjectionValue(expr = AggregateSupportRewriter().rewriteExprNode(projection.expr))

    override fun rewriteSelectHaving(node: ExprNode): ExprNode = registerIdAdder.rewriteExprNode(node)

    override fun rewriteSelectMetas(selectExpr: Select): MetaContainer =
        super.rewriteSelectMetas(selectExpr).add(AggregateCallSiteListMeta(aggregateCallSites.toList()))
}

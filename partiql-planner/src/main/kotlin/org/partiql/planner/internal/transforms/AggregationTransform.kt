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

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SelectValue
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.planner.internal.Env

/**
 * Handles aggregation transformation for both SELECT, HAVING, and ORDER BY clauses.
 * Rewrites their node by replacing (and extracting) each aggregation `i` with a synthetic field name `$agg_i`.
 */
internal object AggregationTransform : AstRewriter<AggregationTransform.Context>() {
    data class Context(
        val aggregations: MutableList<ExprCall>,
        val keys: List<GroupBy.Key>,
        val env: Env
    )

    data class Result(val body: QueryBody.SFW, val orderBy: OrderBy?, val aggregations: MutableList<ExprCall>)

    fun apply(body: QueryBody.SFW, orderBy: OrderBy?, env: Env): Result {
        val aggregations = mutableListOf<ExprCall>()
        val newBody = applySFW(body, aggregations, env)
        val newOrderBy = orderBy?.let { applyOrderBy(orderBy, aggregations, env) }
        return Result(newBody, newOrderBy, aggregations)
    }

    private fun applySFW(node: QueryBody.SFW, aggregations: MutableList<ExprCall>, env: Env): QueryBody.SFW {
        val keys = node.groupBy?.keys ?: emptyList()
        val context = Context(aggregations, keys, env)

        // We support aggregation in select and having only in SFW body. We should skip visiting other clauses to avoid unexpected behavior.
        val select = visitSelect(node.select, context) as Select
        val having = node.having?.let { visitExpr(it, context) as Expr? }

        return if (select !== node.select || having !== node.having) {
            QueryBody.SFW(select, node.exclude, node.from, node.let, node.where, node.groupBy, having, node.window)
        } else {
            node
        }
    }

    private fun applyOrderBy(node: OrderBy, aggregations: MutableList<ExprCall>, env: Env): OrderBy {
        val context = Context(aggregations, emptyList(), env)
        return super.visitOrderBy(node, context) as OrderBy
    }

    override fun visitExprCall(node: ExprCall, ctx: Context) =
        when (isAggregateCall(node, ctx)) {
            true -> {
                val id = Identifier.delimited(syntheticAgg(ctx.aggregations.size))
                ctx.aggregations += node
                exprVarRef(id, isQualified = false)
            }
            else -> node
        }

    fun isAggregateCall(node: ExprCall, ctx: Context): Boolean {
        val fnName = node.function.identifier.text.lowercase()
        val isScalar = ctx.env.hasFn(fnName)
        val isAggregate = if (fnName == "count" && node.args.isEmpty()) {
            // Yet another special case for `COUNT(*)`
            ctx.env.getAggCandidates(fnName, node.args.size + 1).isNotEmpty()
        } else {
            ctx.env.getAggCandidates(fnName, node.args.size).isNotEmpty()
        }

        if (isAggregate && isScalar) {
            throw IllegalStateException("Name registered as both a scalar and aggregate call: `$fnName`")
        }

        return isAggregate
    }

    fun syntheticAgg(i: Int) = "\$agg_$i"

    override fun visitSelectValue(node: SelectValue, ctx: Context): AstNode {
        val visited = super.visitSelectValue(node, ctx)
        val substitutions = ctx.keys.associate {
            it.expr to exprVarRef(Identifier.regular(it.asAlias!!.text), isQualified = false)
        }
        return SubstitutionVisitor.visit(visited, substitutions)
    }

    // only rewrite top-level SFW
    override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: Context): AstNode = node

    override fun defaultReturn(node: AstNode, context: Context) = node
}

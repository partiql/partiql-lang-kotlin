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
import org.partiql.lang.eval.*

/**
 * This rewrite must execute after [GroupByItemAliasRewriter] and [FromSourceAliasRewriter].
 */
class GroupByPathExpressionRewriter(
    parentSubstitutions: Map<ExprNode, SubstitutionPair> = mapOf())
    : SubstitutionRewriter(parentSubstitutions) {

    companion object {
        /**
         * Determines if [gbi] is an expression of the type that should be replaced elsewhere in the query.
         *
         * Since we are only concerned about SQL-92 compatibility here, we only replace path expressions that
         * have a single component, i.e. `f.bar` but not `f.bar.bat`.  (The latter is not part of SQL-92.)
         */
        fun canBeSubstituted(gbi: GroupByItem): Boolean {
            val (expr, asName) = gbi

            //(This is the reason this rewrite needs to execute after [GroupByItemAliasRewriter].)
            return when {
                asName == null                                 -> throw IllegalStateException("GroupByItem.asName must be specified for this rewrite to work")
                !asName.metas.hasMeta(IsSyntheticNameMeta.TAG) ->
                    // If this meta is not present it would indicate that the alias was explicitly specifed, which is
                    // not allowed by SQL-92, so ignore.
                    false

                // Group by expressions other than paths aren't part of SQL-92 so ignore
                expr !is Path                                  -> false

                // Path expressions containing more than one component (i.e. `one.two.three`) are also not part of SQL-92
                expr.components.size != 1                      -> false
                else                                           -> true
            }

        }

        /**
         * Collects all of the aliases defined by the specified [FromSource] and its children.
         * This is why this rewrite must occur after [FromSourceAliasRewriter].
         */
        fun collectAliases(fromSource: FromSource): List<String> =
            when (fromSource) {
                is FromSourceExpr    ->
                    listOf(
                        fromSource.variables.asName?.name
                        ?: errNoContext("FromSourceItem.variables.asName must be specified for this rewrite to work", internal = true))

                is FromSourceJoin    ->
                    collectAliases(fromSource.leftRef) + collectAliases(fromSource.rightRef)

                is FromSourceUnpivot ->
                    listOfNotNull(fromSource.variables.asName?.name, fromSource.variables.atName?.name)
            }
    }

    override fun rewriteSelect(selectExpr: Select): ExprNode {
        val fromSourceAliases = collectAliases(selectExpr.from)

        // These are substitutions for path expressions that do not contain references to shadowed variables
        val unshadowedSubstitutions = getSubstitutionsExceptFor(fromSourceAliases)

        // A rewriter for the above
        val unshadowedRewriter = GroupByPathExpressionRewriter(unshadowedSubstitutions)

        // These are the substitutions originating from the GROUP BY clause of the current [Select] node.
        val currentSubstitutions = getSubstitutionsForSelect(selectExpr)

        // A rewriter for both of the sets of the substitutions defined above.
        val currentAndUnshadowedRewriter = GroupByPathExpressionRewriter(
            unshadowedSubstitutions + currentSubstitutions)

        // Now actually rewrite the query using the appropriate rewriter for each of various clauses of the
        // SELECT statement.

        val projection = currentAndUnshadowedRewriter.rewriteSelectProjection(selectExpr.projection)

        // The scope of the expressions in the FROM clause is the same as that of the parent scope.
        val from = this.rewriteFromSource(selectExpr.from)

        val where = selectExpr.where?.let { unshadowedRewriter.rewriteSelectWhere(it) }

        val groupBy = selectExpr.groupBy?.let { unshadowedRewriter.rewriteGroupBy(it) }

        val having = selectExpr.having?.let { currentAndUnshadowedRewriter.rewriteSelectHaving(it) }

        val limit = selectExpr.limit?.let { unshadowedRewriter.rewriteSelectLimit(it) }

        val metas = unshadowedRewriter.rewriteSelectMetas(selectExpr)

        return Select(
            setQuantifier = selectExpr.setQuantifier,
            projection = projection,
            from = from,
            where = where,
            groupBy = groupBy,
            having = having,
            limit = limit,
            metas = metas)
    }

    private fun getSubstitutionsForSelect(selectExpr: Select): Map<ExprNode, SubstitutionPair> {
        return (selectExpr.groupBy?.groupByItems ?: listOf())
            .asSequence()
            .filter { gbi -> canBeSubstituted(gbi) }
            .map { gbi ->
                val uniqueIdentiferMeta = gbi.asName?.metas?.get(UniqueNameMeta.TAG) as UniqueNameMeta
                SubstitutionPair(
                    MetaStrippingRewriter.stripMetas(gbi.expr),
                    VariableReference(
                        id = gbi.asName.name,
                        case = CaseSensitivity.SENSITIVE,
                        scopeQualifier = ScopeQualifier.UNQUALIFIED,
                        metas = gbi.expr.metas.add(uniqueIdentiferMeta)))
            }.associateBy { it.target }
    }

    private fun getSubstitutionsExceptFor(fromSourceAliases: List<String>): Map<ExprNode, SubstitutionPair> {
        return super.substitutions.values.filter {
            val targetRootVarRef = (it.target as? Path)?.root as? VariableReference
            when (targetRootVarRef) {
                null -> true
                else -> {
                    val ignoreCase = targetRootVarRef.case == CaseSensitivity.INSENSITIVE
                    fromSourceAliases.all { alias ->
                        when (targetRootVarRef) {
                            null -> true // this branch should never execute but we should handle it if it does.
                            else -> targetRootVarRef.id.compareTo(alias, ignoreCase) != 0
                        }
                    }
                }
            }
        }.associateBy { it.target }
    }

    //do not rewrite CallAgg nodes.
    override fun rewriteCallAgg(node: CallAgg): ExprNode = node
}
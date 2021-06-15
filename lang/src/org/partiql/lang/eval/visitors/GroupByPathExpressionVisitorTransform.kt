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

import com.amazon.ionelement.api.metaContainerOf
import org.partiql.lang.ast.IsSyntheticNameMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.errNoContext

/**
 * This transform must execute after [GroupByItemAliasVisitorTransform] and [FromSourceAliasVisitorTransform].
 */
class GroupByPathExpressionVisitorTransform(
    parentSubstitutions: Map<PartiqlAst.Expr, SubstitutionPair> = mapOf())
    : SubstitutionVisitorTransform(parentSubstitutions) {

    companion object {
        /**
         * Determines if [gbi] is an expression of the type that should be replaced elsewhere in the query.
         *
         * Since we are only concerned about SQL-92 compatibility here, we only replace path expressions that
         * have a single component, i.e. `f.bar` but not `f.bar.bat`.  (The latter is not part of SQL-92.)
         */
        fun canBeSubstituted(groupKey: PartiqlAst.GroupKey): Boolean {
            val expr = groupKey.expr
            val asName = groupKey.asAlias

            //(This is the reason this transform needs to execute after [GroupByItemAliasVisitorTransform].)
            return when {
                asName == null                                     -> throw IllegalStateException("GroupByItem.asName must be specified for this transform to work")
                !asName.metas.containsKey(IsSyntheticNameMeta.TAG) ->
                    // If this meta is not present it would indicate that the alias was explicitly specifed, which is
                    // not allowed by SQL-92, so ignore.
                    false

                // Group by expressions other than paths aren't part of SQL-92 so ignore
                expr !is PartiqlAst.Expr.Path                      -> false

                // Path expressions containing more than one component (i.e. `one.two.three`) are also not part of SQL-92
                expr.steps.size != 1                               -> false
                else                                               -> true
            }

        }

        /**
         * Collects all of the aliases defined by the specified [FromSource] and its children.
         * This is why this transform must occur after [FromSourceAliasVisitorTransform].
         */
        fun collectAliases(fromSource: PartiqlAst.FromSource): List<String> =
            when (fromSource) {
                is PartiqlAst.FromSource.Scan    ->
                    listOf(
                        fromSource.asAlias?.text
                        ?: errNoContext("FromSourceItem.variables.asName must be specified for this transform to work", internal = true))

                is PartiqlAst.FromSource.Join    ->
                    collectAliases(fromSource.left) + collectAliases(fromSource.right)

                is PartiqlAst.FromSource.Unpivot ->
                    listOfNotNull(fromSource.asAlias?.text, fromSource.atAlias?.text)
            }
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val fromSourceAliases = collectAliases(node.from)

        // These are substitutions for path expressions that do not contain references to shadowed variables
        val unshadowedSubstitutions = getSubstitutionsExceptFor(fromSourceAliases)

        // A transformer for the above
        val unshadowedTransformer = GroupByPathExpressionVisitorTransform(unshadowedSubstitutions)

        // These are the substitutions originating from the GROUP BY clause of the current [Select] node.
        val currentSubstitutions = getSubstitutionsForSelect(node)

        // A transformer for both of the sets of the substitutions defined above.
        val currentAndUnshadowedTransformer = GroupByPathExpressionVisitorTransform(
            unshadowedSubstitutions + currentSubstitutions)

        // Now actually transform the query using the appropriate transformer for each of various clauses of the
        // SELECT statement.

        val projection = currentAndUnshadowedTransformer.transformExprSelect_project(node)

        // The scope of the expressions in the FROM clause is the same as that of the parent scope.
        val from = this.transformExprSelect_from(node)
        val fromLet = unshadowedTransformer.transformExprSelect_fromLet(node)
        val where = unshadowedTransformer.transformExprSelect_where(node)
        val groupBy = unshadowedTransformer.transformExprSelect_group(node)
        val having = currentAndUnshadowedTransformer.transformExprSelect_having(node)
        val order = currentAndUnshadowedTransformer.transformExprSelect_order(node)
        val limit = unshadowedTransformer.transformExprSelect_limit(node)
        val metas = unshadowedTransformer.transformExprSelect_metas(node)

        return PartiqlAst.build {
            PartiqlAst.Expr.Select(
                setq = node.setq,
                project = projection,
                from = from,
                fromLet = fromLet,
                where = where,
                group = groupBy,
                having = having,
                order = order,
                limit = limit,
                metas = metas)
        }
    }

    private fun getSubstitutionsForSelect(selectExpr: PartiqlAst.Expr.Select): Map<PartiqlAst.Expr, SubstitutionPair> {
        return (selectExpr.group?.keyList?.keys ?: listOf())
            .asSequence()
            .filter { groupKey -> canBeSubstituted(groupKey) }
            .map { groupKey ->
                val uniqueIdentifierMeta = groupKey.asAlias?.metas?.get(UniqueNameMeta.TAG) as UniqueNameMeta
                SubstitutionPair(
                    groupKey.expr,
                    PartiqlAst.build {
                        id(
                            name = groupKey.asAlias.text,
                            case = caseSensitive(),
                            qualifier = unqualified(),
                            metas = groupKey.expr.metas + metaContainerOf(UniqueNameMeta.TAG to uniqueIdentifierMeta))
                    })
            }.associateBy { it.target }
    }

    private fun getSubstitutionsExceptFor(fromSourceAliases: List<String>): Map<PartiqlAst.Expr, SubstitutionPair> {
        return super.substitutions.values.filter {
            val targetRootVarRef = (it.target as? PartiqlAst.Expr.Path)?.root as? PartiqlAst.Expr.Id
            when (targetRootVarRef) {
                null -> true
                else -> {
                    val ignoreCase = targetRootVarRef.case is PartiqlAst.CaseSensitivity.CaseInsensitive
                    fromSourceAliases.all { alias ->
                        when (targetRootVarRef) {
                            null -> true // this branch should never execute but we should handle it if it does.
                            else -> targetRootVarRef.name.text.compareTo(alias, ignoreCase) != 0
                        }
                    }
                }
            }
        }.associateBy { it.target }
    }

    // do not transform CallAgg nodes.
    override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr = node

}

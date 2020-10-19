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
import org.partiql.lang.eval.extractColumnAlias
import org.partiql.pig.runtime.SymbolPrimitive


/**
 * Pre-calculates [PartiqlAst.GroupBy] aliases, while not changing any that were previously specified, for example:
 *
 * `SELECT * FROM a GROUP BY a.b AS foo, a.c, 2 + 3` becomes
 * `SELECT * FROM a GROUP BY a.b AS foo, a.c AS c, 2 + 3 as _3`.
 *
 * Also adds [UniqueNameMeta] to [PartiqlAst.GroupBy.keyList].  In order for the generated unique names to be correct,
 * this rewrite must be presented with only the top-most node in a query.
 *
 * If provided with a query with all of the group by item aliases already specified, an exact clone is returned.
 */
class GroupByItemAliasVisitorTransform(var nestLevel: Int = 0) : PartiqlAst.VisitorTransform() {

    override fun transformGroupBy(node: PartiqlAst.GroupBy): PartiqlAst.GroupBy {
        return PartiqlAst.build {
            groupBy_(
                strategy = node.strategy,
                keyList = PartiqlAst.GroupKeyList(node.keyList.keys.mapIndexed { index, it ->
                    val aliasText = it.asAlias?.text ?: it.expr.extractColumnAlias(index)
                    var metas = it.expr.metas + metaContainerOf(
                        UniqueNameMeta.TAG to UniqueNameMeta("\$__partiql__group_by_${nestLevel}_item_$index"))

                    if (it.asAlias == null) {
                        metas = metas + metaContainerOf(IsSyntheticNameMeta.TAG to IsSyntheticNameMeta.instance)
                    }
                    val alias = SymbolPrimitive(aliasText, metas)

                    groupKey_(transformExpr(it.expr), alias, alias.metas)
                }, node.keyList.metas),
                groupAsAlias = node.groupAsAlias?.let { transformSymbolPrimitive(it) },
                metas = node.metas)
        }
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        nestLevel++
        val new_setq = transformExprSelect_setq(node)
        val new_project = transformExprSelect_project(node)
        val new_from = transformExprSelect_from(node)
        val new_fromLet = transformExprSelect_fromLet(node)
        val new_where = transformExprSelect_where(node)
        val new_group = transformExprSelect_group(node)
        val new_having = transformExprSelect_having(node)
        val new_limit = transformExprSelect_limit(node)
        val new_metas = transformExprSelect_metas(node)
        nestLevel--
        return PartiqlAst.build {
            PartiqlAst.Expr.Select(
                setq = new_setq,
                project = new_project,
                from = new_from,
                fromLet = new_fromLet,
                where = new_where,
                group = new_group,
                having = new_having,
                limit = new_limit,
                metas = new_metas
            )
        }
    }
}

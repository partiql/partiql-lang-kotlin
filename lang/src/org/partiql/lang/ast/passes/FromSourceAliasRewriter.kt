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
 * Assigns aliases to any [FromSourceExpr] that does not already have one.
 *
 * For example: `SELECT * FROM foo` gets rewritten as `SELECT * from foo as foo`.
 * Path expressions:  `SELECT * FROM foo.bar.bat` gets rewritten as `SELECT * from foo.bar.bat as bat`
 *
 * If provided with a query that has all of the from source aliases already specified, an exact clone is returned.
 */
class FromSourceAliasRewriter : AstRewriterBase() {

    private class InnerFromSourceAliasRewriter : AstRewriterBase() {
        private var fromSourceCounter = 0
        private fun extractSourceLocationMetaContainer(metas: MetaContainer): MetaContainer {
            val found = metas.find(SourceLocationMeta.TAG) ?: return metaContainerOf()
            return metaContainerOf(found)
        }

        override fun rewriteFromSourceExpr(fromSource: FromSourceExpr): FromSourceExpr {
            // Note: yes this function is identical to [rewriteFromSourceUnpivot]. This is because [FromSourceExpr]
            // and [FromSourceUnpivot] have almost the same properties with the difference being that [FromSourceUnpivot]
            // has an additional `metas` property while [FromSourceExpr] does not.  We considered merging these
            // two types so that this duplication isn't needed but have determined not to do so at this time and attempts
            // to deduplicate through other means were rather convoluted, creating more problems than they solved.
            // https://github.com/partiql/partiql-lang-kotlin/issues/39

            val thisFromSourceIndex = fromSourceCounter++
            val newExpr = super.rewriteExprNode(fromSource.expr)
            return when {
                fromSource.asName != null -> fromSource.copy(expr = newExpr)
                else                      -> {
                    val fromSourceAlias = fromSource.expr.extractColumnAlias(thisFromSourceIndex)
                    val asName = SymbolicName(fromSourceAlias,
                                              extractSourceLocationMetaContainer(fromSource.expr.metas))
                    fromSource.copy(expr = newExpr, asName = asName)
                }
            }
        }

        override fun rewriteFromSourceUnpivot(fromSource: FromSourceUnpivot): FromSourceUnpivot {
            val thisFromSourceIndex = fromSourceCounter++
            val newExpr = super.rewriteExprNode(fromSource.expr)
            return when {
                fromSource.asName != null -> fromSource.copy(expr = newExpr)
                else                      -> {
                    val fromSourceAlias = fromSource.expr.extractColumnAlias(thisFromSourceIndex)
                    val asName = SymbolicName(fromSourceAlias,
                                              extractSourceLocationMetaContainer(fromSource.expr.metas))
                    fromSource.copy(expr = newExpr, asName = asName)
                }
            }
        }

        override fun rewriteSelect(selectExpr: Select): ExprNode =
            //We need to make sure to use a different [fromSourceCounter] for sub-queries.
            FromSourceAliasRewriter().rewriteSelect(selectExpr)

    }

    override fun rewriteFromSource(fromSource: FromSource): FromSource =
        InnerFromSourceAliasRewriter().rewriteFromSource(fromSource)

}
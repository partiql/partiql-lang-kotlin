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
//TODO:  consider renaming this class (and inner class) since we've introduced the [LetVariables] class.
class FromSourceAliasRewriter : AstRewriterBase() {

    private class InnerFromSourceAliasRewriter : AstRewriterBase() {
        private var fromSourceCounter = 0

        override fun rewriteFromSourceLet(fromSourceLet: FromSourceLet): FromSourceLet {
            val thisFromSourceIndex = fromSourceCounter++
            val newFromSource = super.rewriteFromSourceLet(fromSourceLet)
            return if (newFromSource.variables.asName != null) {
                newFromSource
            } else {
                val fromSourceAlias = fromSourceLet.expr.extractColumnAlias(thisFromSourceIndex)
                val asAlias = SymbolicName(
                    fromSourceAlias,
                    fromSourceLet.expr.metas.sourceLocationContainer
                )
                newFromSource.copy(newVariables = newFromSource.variables.copy(asName = asAlias))
            }
        }

        override fun rewriteSelect(selectExpr: Select): ExprNode =
            //We need to make sure to use a different [fromSourceCounter] for sub-queries.
            FromSourceAliasRewriter().rewriteSelect(selectExpr)

    }

    override fun rewriteFromSource(fromSource: FromSource): FromSource =
        InnerFromSourceAliasRewriter().rewriteFromSource(fromSource)

}
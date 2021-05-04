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
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.extractSourceLocation

/**
 * Specifies an individual substitution to be performed by [SubstitutionVisitorTransform].
 *
 * [target] will be replaced with [replacement].  If the original node has an instance of [SourceLocationMeta], that is
 * copied to the replacement as well.
 *
 * [target] should have its metas stripped as metas will effect the results of the equivalence check.
 */
data class SubstitutionPair(val target: PartiqlAst.Expr, val replacement: PartiqlAst.Expr)

/**
 * Given a [Map<ExprNode, SubstitutionPair>] ([substitutions]), replaces every node of the AST that is
 * equivalent to a [SubstitutionPair.target] with its corresponding [SubstitutionPair.replacement].
 *
 * This class is `open` to allow subclasses to restrict the nodes to which the substitution should occur.
 */
open class SubstitutionVisitorTransform(protected val substitutions: Map<PartiqlAst.Expr, SubstitutionPair>): VisitorTransformBase() {

    /**
     * If [node] matches any of the target nodes in [substitutions], replaces the node with the replacement.
     *
     * If [node] has a [SourceLocationMeta], the replacement is cloned and the [SourceLocationMeta] is copied to the
     * clone.
     */
    override fun transformExpr(node: PartiqlAst.Expr): PartiqlAst.Expr {
        val matchingSubstitution = substitutions[node]

        return matchingSubstitution?.let { ms ->
            node.extractSourceLocation().let {
                sl -> MetaVisitorTransform(sl).transformExpr(ms.replacement)
            }
        } ?: super.transformExpr(node)
    }

    /**
     * Class creates a copy of [PartiqlAst.Expr], but uses [newMetas] as the metas.
     *
     * After .copy() and copying metas is added to PIG (https://github.com/partiql/partiql-ir-generator/pull/53) change
     * this and its usages to use .copy().
     */
    inner class MetaVisitorTransform(private val newMetas: MetaContainer) : VisitorTransformBase() {
        override fun transformMetas(metas: MetaContainer): MetaContainer = newMetas
    }

}

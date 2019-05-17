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
 * Specifies an individual substitution to be performed by [SubstitutionRewriter].
 *
 * [target] will be replaced with [replacement].  If the original node has an instance of [SourceLocationMeta], that is
 * copied to the replacement as well.
 *
 * [target] should have its metas stripped as metas will effect the results of the equivalence check.
 */
data class SubstitutionPair(val target: ExprNode, val replacement: ExprNode)

/**
 * Given a [Map<ExprNode, SubstitutionPair>] ([substitutions]), replaces every node of the AST that is
 * equivalent to a [SubstitutionPair.target] with its corresponding [SubstitutionPair.replacement].
 *
 * This class is `open` to allow subclasses to restrict the nodes to which the substitution should occur.
 */
open class SubstitutionRewriter(protected val substitutions: Map<ExprNode, SubstitutionPair>): AstRewriterBase() {

    /**
     * If [node] matches any of the target nodes in [substitutions], replaces the node with the replacement.
     *
     * If [node] has a [SourceLocationMeta], the replacement is cloned and the [SourceLocationMeta] is copied to the
     * clone.
     */
    override fun rewriteExprNode(node: ExprNode): ExprNode {
        // It is currently necessary to strip the meta information because meta information is included as part of
        // equivalence, and different [SourceLocationMeta] (among others) will always cause the equivalence check to
        // be `false`.
        val candidate = MetaStrippingRewriter.stripMetas(node)

        val matchingSubstitution = substitutions[candidate]

        return matchingSubstitution?.let { ms ->
            node.metas.sourceLocation?.let { sl -> ms.replacement.copy(metaContainerOf(sl)) } ?: ms.replacement
        } ?: super.rewriteExprNode(node)
    }
}


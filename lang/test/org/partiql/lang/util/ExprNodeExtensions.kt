// Don't need warnings about ExprNode deprecation.
@file:Suppress("DEPRECATION")

package org.partiql.lang.util

import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.passes.MetaStrippingRewriter

@Suppress("DEPRECATION")
@Deprecated(
    "Will be removed after existing tests no longer require stripping metas",
    replaceWith = ReplaceWith("No replacement, not needed anymore")
)
fun ExprNode.stripMetas() = MetaStrippingRewriter.stripMetas(this)

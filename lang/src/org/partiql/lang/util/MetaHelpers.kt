package org.partiql.lang.util

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.Meta

/** Constructs a container with the specified metas implementing [Meta] interface. */
fun metaContainerOf(vararg metas: Meta): MetaContainer =
    com.amazon.ionelement.api.metaContainerOf(metas.map { it.tag to it })


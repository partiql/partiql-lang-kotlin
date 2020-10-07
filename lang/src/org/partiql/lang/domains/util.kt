package org.partiql.lang.domains

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.lang.ast.SourceLocationMeta

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlAst.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())


/**
 * Returns a [MetaContainer] with *only* the source location of the receiver [MetaContainer], if present.
 *
 * Avoids creating a new [MetaContainer] if its not needed.
 */
fun PartiqlAst.PartiqlAstNode.extractSourceLocation(): MetaContainer {
    return when (this.metas.size) {
        0 -> emptyMetaContainer()
        1 -> when {
            this.metas.containsKey(SourceLocationMeta.TAG) -> this.metas
            else -> emptyMetaContainer()
        }
        else -> {
            this.metas[SourceLocationMeta.TAG]?.let { metaContainerOf(SourceLocationMeta.TAG to it) }
                ?: emptyMetaContainer()
        }
    }
}
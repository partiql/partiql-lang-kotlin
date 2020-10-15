package org.partiql.lang.domains

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.BindingCase

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

fun PropertyValueMap.addSourceLocation(metas: MetaContainer): PropertyValueMap {
    (metas[SourceLocationMeta.TAG] as? SourceLocationMeta)?.let {
        this[Property.LINE_NUMBER] = it.lineNum
        this[Property.COLUMN_NUMBER] = it.charOffset
    }
    return this
}

fun PartiqlAst.CaseSensitivity.toBindingCase(): BindingCase = when(this) {
    is PartiqlAst.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlAst.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}
package org.partiql.lang.domains

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.lang.ast.Meta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.BindingCase

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlAst.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlLogical.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlLogical.Builder.pathExpr(exp: PartiqlLogical.Expr) =
    pathExpr(exp, caseInsensitive())

// Workaround for a bug in PIG that is fixed in its next release:
// https://github.com/partiql/partiql-ir-generator/issues/41
fun List<IonElement>.asAnyElement() =
    this.map { it.asAnyElement() }

val MetaContainer.staticType: StaticTypeMeta? get() = this[StaticTypeMeta.TAG] as StaticTypeMeta?

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

/**
 * Adds [Property.LINE_NUMBER] and [Property.COLUMN_NUMBER] to the [PropertyValueMap] if the [SourceLocationMeta.TAG]
 * is present in the passed [metas]. Otherwise, returns the unchanged [PropertyValueMap].
 */
fun PropertyValueMap.addSourceLocation(metas: MetaContainer): PropertyValueMap {
    (metas[SourceLocationMeta.TAG] as? SourceLocationMeta)?.let {
        this[Property.LINE_NUMBER] = it.lineNum
        this[Property.COLUMN_NUMBER] = it.charOffset
    }
    return this
}

/**
 * Converts a [PartiqlAst.CaseSensitivity] to a [BindingCase].
 */
fun PartiqlAst.CaseSensitivity.toBindingCase(): BindingCase = when (this) {
    is PartiqlAst.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlAst.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

/**
 * Converts a [PartiqlLogical.CaseSensitivity] to a [BindingCase].
 */
fun PartiqlLogical.CaseSensitivity.toBindingCase(): BindingCase = when(this) {
    is PartiqlLogical.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlLogical.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

/**
 * Converts a [PartiqlLogical.CaseSensitivity] to a [BindingCase].
 */
fun PartiqlPhysical.CaseSensitivity.toBindingCase(): BindingCase = when(this) {
    is PartiqlPhysical.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlPhysical.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

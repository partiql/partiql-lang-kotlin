package org.partiql.lang.domains

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.Ident
import org.partiql.lang.ast.Meta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlAst.Builder.vr(name: String) =
    vr(id(name, regular()), unqualified())

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlLogical.Builder.vr(name: String) =
    vr(id(name, regular()), unqualified())

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlLogical.Builder.pathExpr(exp: PartiqlLogical.Expr) =
    pathExpr(exp, regular())

val MetaContainer.staticType: StaticTypeMeta? get() = this[StaticTypeMeta.TAG] as StaticTypeMeta?

/** Constructs a container with the specified metas. */
fun metaContainerOf(vararg metas: Meta): MetaContainer =
    metaContainerOf(metas.map { Pair(it.tag, it) })

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
 * Converts a [PartiqlAst.IdKind] to a [BindingCase].
 */
fun PartiqlAst.IdKind.toBindingCase(): BindingCase = when (this) {
    is PartiqlAst.IdKind.Regular -> BindingCase.INSENSITIVE
    is PartiqlAst.IdKind.Delimited -> BindingCase.SENSITIVE
}

/**
 * Converts a [PartiqlLogical.IdKind] to a [BindingCase].
 */
fun PartiqlLogical.IdKind.toBindingCase(): BindingCase = when (this) {
    is PartiqlLogical.IdKind.Regular -> BindingCase.INSENSITIVE
    is PartiqlLogical.IdKind.Delimited -> BindingCase.SENSITIVE
}

/**
 * Converts a [PartiqlLogical.IdKind] to a [BindingCase].
 */
fun PartiqlPhysical.IdKind.toBindingCase(): BindingCase = when (this) {
    is PartiqlPhysical.IdKind.Regular -> BindingCase.INSENSITIVE
    is PartiqlPhysical.IdKind.Delimited -> BindingCase.SENSITIVE
}

fun PartiqlAst.Id.toBindingName(): BindingName =
    BindingName(this.symb.text, this.kind.toBindingCase())

fun PartiqlLogical.Id.toBindingName(): BindingName =
    BindingName(this.symb.text, this.kind.toBindingCase())

fun PartiqlAst.Defnid.toIdent(): Ident =
    Ident.createAsIs(this.symb.text)

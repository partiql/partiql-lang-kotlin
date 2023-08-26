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

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlAst.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlLogical.Builder.id(name: String) =
    id(name, caseInsensitive(), unqualified())

// TODO:  once https://github.com/partiql/partiql-ir-generator/issues/6 has been completed, we can delete this.
fun PartiqlLogical.Builder.pathExpr(exp: PartiqlLogical.Expr) =
    pathExpr(exp, caseInsensitive())

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
 * Converts a [PartiqlAst.CaseSensitivity] to a [BindingCase].
 */
fun PartiqlAst.CaseSensitivity.toBindingCase(): BindingCase = when (this) {
    is PartiqlAst.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlAst.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

/**
 * Converts a [PartiqlLogical.CaseSensitivity] to a [BindingCase].
 */
fun PartiqlLogical.CaseSensitivity.toBindingCase(): BindingCase = when (this) {
    is PartiqlLogical.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlLogical.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

/**
 * Converts a [PartiqlLogical.CaseSensitivity] to a [BindingCase].
 */
fun PartiqlPhysical.CaseSensitivity.toBindingCase(): BindingCase = when (this) {
    is PartiqlPhysical.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlPhysical.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

fun PartiqlAst.Defnid.toIdent(): Ident =
    Ident.createAsIs(this.symb.text)

/** wVG This is a bridge method, indicating places from which introduction of semantic identifiers should spread further.
 *  Given e: SymbolPrimitive (formerly) or e: PartiqlAst.Defnid (currently),
 *  the former usage pattern was
 *      e.text         // results in a String
 *  which now, with this bridge method, became
 *      e.string()     // results is a String
 *  and should, in the future, transition to something like
 *      e.ident()      // results in an identifier
 *  with the downstream code using identifiers rather than strings.
 */
fun PartiqlAst.Defnid.string(): String =
    this.symb.text

fun PartiqlLogicalResolved.Defnid.string(): String =
    this.symb.text

fun PartiqlPhysical.Defnid.string(): String =
    this.symb.text

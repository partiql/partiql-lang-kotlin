package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.SymbolValue
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class SymbolValueImpl(
    override val value: String?,
    override val annotations: PersistentList<String>,
) : SymbolValue() {

    override fun copy(annotations: Annotations) = SymbolValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): SymbolValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): SymbolValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitSymbol(this, ctx)
}

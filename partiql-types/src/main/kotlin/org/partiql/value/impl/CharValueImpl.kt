package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.CharValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class CharValueImpl(
    override val value: Char?,
    override val annotations: PersistentList<String>,
) : CharValue() {

    override fun copy(annotations: Annotations) = CharValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): CharValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): CharValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitChar(this, ctx)
}

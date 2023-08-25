package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.Int16Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class Int16ValueImpl(
    override val value: Short?,
    override val annotations: PersistentList<String>,
) : Int16Value() {

    override fun copy(annotations: Annotations) = Int16ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int16Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int16Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInt16(this, ctx)
}

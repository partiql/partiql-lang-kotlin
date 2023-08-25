package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.Float64Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class Float64ValueImpl(
    override val value: Double?,
    override val annotations: PersistentList<String>,
) : Float64Value() {
    override fun copy(annotations: Annotations) = Float64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Float64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Float64Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitFloat64(this, ctx)
}

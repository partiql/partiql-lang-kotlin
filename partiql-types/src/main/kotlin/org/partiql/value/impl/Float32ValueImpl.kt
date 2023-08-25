package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.Float32Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class Float32ValueImpl(
    override val value: Float?,
    override val annotations: PersistentList<String>,
) : Float32Value() {

    override fun copy(annotations: Annotations) = Float32ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Float32Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Float32Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitFloat32(this, ctx)
}

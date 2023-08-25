package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class Int32ValueImpl(
    override val value: Int?,
    override val annotations: PersistentList<String>,
) : Int32Value() {

    override fun copy(annotations: Annotations) = Int32ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int32Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int32Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInt32(this, ctx)
}

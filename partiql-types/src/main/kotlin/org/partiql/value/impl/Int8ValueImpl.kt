package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.Int8Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class Int8ValueImpl(
    override val value: Byte?,
    override val annotations: PersistentList<String>,
) : Int8Value() {

    override fun copy(annotations: Annotations) = Int8ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int8Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int8Value = _withoutAnnotations()
    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInt8(this, ctx)
}

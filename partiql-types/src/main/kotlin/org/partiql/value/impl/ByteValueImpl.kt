package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.ByteValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class ByteValueImpl(
    override val value: Byte?,
    override val annotations: PersistentList<String>,
) : ByteValue() {
    override fun copy(annotations: Annotations) = ByteValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): ByteValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): ByteValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitByte(this, ctx)
}

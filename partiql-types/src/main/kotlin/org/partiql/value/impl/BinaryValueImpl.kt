package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.BinaryValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor
import java.util.BitSet

@OptIn(PartiQLValueExperimental::class)
internal data class BinaryValueImpl(
    override val value: BitSet?,
    override val annotations: PersistentList<String>,
) : BinaryValue() {
    override fun copy(annotations: Annotations) = BinaryValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BinaryValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): BinaryValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitBinary(this, ctx)
}

package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class ClobValueImpl(
    override val value: ByteArray?,
    override val annotations: PersistentList<String>,
) : ClobValue() {
    override fun copy(annotations: Annotations) = ClobValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): ClobValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): ClobValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitClob(this, ctx)
}

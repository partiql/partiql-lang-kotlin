package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.Int64Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class Int64ValueImpl(
    override val value: Long?,
    override val annotations: PersistentList<String>,
) : Int64Value() {
    override fun copy(annotations: Annotations) = Int64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int64Value = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInt64(this, ctx)
}

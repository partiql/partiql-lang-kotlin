package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.IntervalValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class IntervalValueImpl(
    override val value: Long?,
    override val annotations: PersistentList<String>,
) : IntervalValue() {

    override fun copy(annotations: Annotations) = IntervalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): IntervalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): IntervalValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInterval(this, ctx)
}

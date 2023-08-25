package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TimeValue
import org.partiql.value.datetime.Time
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class TimeValueImpl(
    override val value: Time?,
    override val annotations: PersistentList<String>,
) : TimeValue() {

    override fun copy(annotations: Annotations) = TimeValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): TimeValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): TimeValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitTime(this, ctx)
}

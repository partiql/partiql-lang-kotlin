package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TimestampValue
import org.partiql.value.datetime.Timestamp
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class TimestampValueImpl(
    override val value: Timestamp?,
    override val annotations: PersistentList<String>,
) : TimestampValue() {

    override fun copy(annotations: Annotations) = TimestampValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): TimestampValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): TimestampValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitTimestamp(this, ctx)
}

package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.DateValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.datetime.Date
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class DateValueImpl(
    override val value: Date?,
    override val annotations: PersistentList<String>,
) : DateValue() {
    override fun copy(annotations: Annotations) = DateValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): DateValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): DateValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitDate(this, ctx)
}

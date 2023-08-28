package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class BoolValueImpl(
    override val value: Boolean?,
    override val annotations: PersistentList<String>,
) : BoolValue() {

    override fun copy(annotations: Annotations) = BoolValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BoolValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): BoolValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitBool(this, ctx)
}

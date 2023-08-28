package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class MissingValueImpl(
    override val annotations: PersistentList<String>,
) : MissingValue() {

    override fun copy(annotations: Annotations) = MissingValueImpl(annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): MissingValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): MissingValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitMissing(this, ctx)
}

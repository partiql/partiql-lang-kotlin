package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class NullValueImpl(
    override val annotations: PersistentList<String>,
) : NullValue() {

    override fun copy(annotations: Annotations) = NullValueImpl(annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNull(this, ctx)
}

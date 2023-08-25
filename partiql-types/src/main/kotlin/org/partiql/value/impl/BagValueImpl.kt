package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal class BagValueImpl<T : PartiQLValue>(
    private val delegate: Sequence<T>?,
    override val annotations: PersistentList<String>,
) : BagValue<T>() {

    override val elements: Sequence<T>? = delegate

    override fun copy(annotations: Annotations) = BagValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BagValue<T> = _withAnnotations(annotations.toPersistentList())

    override fun withoutAnnotations(): BagValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitBag(this, ctx)
}

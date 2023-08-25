package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal class ListValueImpl<T : PartiQLValue>(
    private val delegate: Sequence<T>?,
    override val annotations: PersistentList<String>,
) : ListValue<T>() {

    override val elements: Sequence<T>? = delegate

    override fun copy(annotations: Annotations) = ListValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): ListValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): ListValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitList(this, ctx)
}

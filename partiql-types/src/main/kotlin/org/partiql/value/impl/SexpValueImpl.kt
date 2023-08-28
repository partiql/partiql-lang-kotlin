package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.SexpValue
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal class SexpValueImpl<T : PartiQLValue>(
    private val delegate: Sequence<T>?,
    override val annotations: PersistentList<String>,
) : SexpValue<T>() {

    override val elements: Sequence<T>? = delegate

    override fun copy(annotations: Annotations) = SexpValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): SexpValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): SexpValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitSexp(this, ctx)
}

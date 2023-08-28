package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class StringValueImpl(
    override val value: String?,
    override val annotations: PersistentList<String>,
) : StringValue() {

    override fun copy(annotations: Annotations) = StringValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StringValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): StringValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitString(this, ctx)
}

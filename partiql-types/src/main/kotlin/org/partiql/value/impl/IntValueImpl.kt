package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigInteger

@OptIn(PartiQLValueExperimental::class)
internal data class IntValueImpl(
    override val value: BigInteger?,
    override val annotations: PersistentList<String>,
) : IntValue() {

    override fun copy(annotations: Annotations) = IntValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): IntValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): IntValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInt(this, ctx)
}

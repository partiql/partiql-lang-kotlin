package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.DecimalValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigDecimal

@OptIn(PartiQLValueExperimental::class)
internal data class DecimalValueImpl(
    override val value: BigDecimal?,
    override val annotations: PersistentList<String>,
) : DecimalValue() {

    override fun copy(annotations: Annotations) = DecimalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): DecimalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): DecimalValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitDecimal(this, ctx)
}

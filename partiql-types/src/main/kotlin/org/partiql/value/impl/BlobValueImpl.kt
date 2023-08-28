package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.BlobValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class BlobValueImpl(
    override val value: ByteArray?,
    override val annotations: PersistentList<String>,
) : BlobValue() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BlobValueImpl
        return value.contentEquals(other.value)
    }

    override fun hashCode() = value.contentHashCode()

    override fun copy(annotations: Annotations) = BlobValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): BlobValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): BlobValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitBlob(this, ctx)
}

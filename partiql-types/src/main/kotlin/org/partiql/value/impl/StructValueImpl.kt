package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.util.PartiQLValueVisitor

/**
 * Implementation of a [StructValue<T>] backed by a Sequence.
 *
 * @param T
 * @property delegate
 * @property annotations
 */
@OptIn(PartiQLValueExperimental::class)
internal data class SequenceStructValueImpl<T : PartiQLValue>(
    private val delegate: Sequence<Pair<String, T>>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val fields: Sequence<Pair<String, T>>? = delegate

    override fun get(key: String): T? {
        TODO("Not yet implemented")
    }

    override fun getAll(key: String): Iterable<T> {
        TODO("Not yet implemented")
    }

    override fun copy(annotations: Annotations) = SequenceStructValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StructValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): StructValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitStruct(this, ctx)
}

/**
 * Implementation of a [StructValue<T>] backed by a multi Map.
 *
 * @param T
 * @property delegate
 * @property annotations
 */
@OptIn(PartiQLValueExperimental::class)
internal data class MultiMapStructValueImpl<T : PartiQLValue>(
    private val delegate: Map<String, Iterable<T>>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val fields: Sequence<Pair<String, T>>?
        get() {
            if (delegate == null) {
                return null
            }
            return delegate.asSequence().map { f -> f.value.map { v -> f.key to v } }.flatten()
        }

    override fun get(key: String): T? = getAll(key).firstOrNull()

    override fun getAll(key: String): Iterable<T> {
        if (delegate == null) {
            throw NullPointerException()
        }
        return delegate[key] ?: return emptyList()
    }

    override fun copy(annotations: Annotations) = MultiMapStructValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StructValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): StructValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitStruct(this, ctx)
}

/**
 * Implementation of a [StructValue<T>] backed by a Map.
 *
 * @param T
 * @property delegate
 * @property annotations
 */
@OptIn(PartiQLValueExperimental::class)
internal data class MapStructValueImpl<T : PartiQLValue>(
    private val delegate: Map<String, T>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val fields: Sequence<Pair<String, T>>?
        get() {
            if (delegate == null) {
                return null
            }
            return delegate.asSequence().map { f -> f.key to f.value }
        }

    override fun get(key: String): T? {
        if (delegate == null) {
            throw NullPointerException()
        }
        return delegate[key]
    }

    override fun getAll(key: String): Iterable<T> {
        val v = get(key)
        return when (v == null) {
            true -> emptyList()
            else -> listOf(v)
        }
    }

    override fun copy(annotations: Annotations) = MapStructValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StructValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): StructValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitStruct(this, ctx)
}

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
internal class SequenceStructValueImpl<T : PartiQLValue>(
    private val delegate: Sequence<Pair<String, T>>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val fields: Sequence<Pair<String, T>>? = delegate

    override operator fun get(key: String): T? {
        if (delegate == null) {
            return null
        }
        return delegate.first { it.first == key }.second
    }

    override fun getAll(key: String): Iterable<T> {
        if (delegate == null) {
            return emptyList()
        }
        return delegate.filter { it.first == key }.map { it.second }.asIterable()
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
internal class MultiMapStructValueImpl<T : PartiQLValue>(
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

    override operator fun get(key: String): T? = getAll(key).firstOrNull()

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
internal class MapStructValueImpl<T : PartiQLValue>(
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

    override operator fun get(key: String): T? {
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

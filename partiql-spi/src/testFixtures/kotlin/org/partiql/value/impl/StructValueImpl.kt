/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.value.impl

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValue
import org.partiql.value.StructValue
import org.partiql.value.util.PartiQLValueVisitor

/**
 * Implementation of a [StructValue<T>] backed by an iterator.
 *
 * @param T
 * @property delegate
 * @property annotations
 */
internal class IterableStructValueImpl<T : PartiQLValue>(
    private val delegate: Iterable<Pair<String, T>>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val isNull: Boolean = delegate == null

    override val fields: Iterable<String>
        get() = delegate!!.map { it.first }

    override val values: Iterable<T>
        get() = delegate!!.map { it.second }

    override val entries: Iterable<Pair<String, T>>
        get() = delegate!!

    override operator fun get(key: String): T? {
        if (delegate == null) {
            return null
        }
        return delegate.firstOrNull { it.first == key }?.second
    }

    override fun getAll(key: String): Iterable<T> {
        if (delegate == null) {
            return emptyList()
        }
        return delegate.filter { it.first == key }.map { it.second }.asIterable()
    }

    override fun copy(annotations: Annotations) = IterableStructValueImpl(delegate, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): StructValue<T> = _withAnnotations(annotations)

    override fun withoutAnnotations(): StructValue<T> = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitStruct(this, ctx)

    override fun toString(): String {
        return delegate?.joinToString(separator = ", ", prefix = "{ ", postfix = " }") {
            "'${it.first}': ${it.second}"
        } ?: "null.struct"
    }
}

/**
 * Implementation of a [StructValue<T>] backed by a multi Map.
 *
 * @param T
 * @property delegate
 * @property annotations
 */
internal class MultiMapStructValueImpl<T : PartiQLValue>(
    private val delegate: Map<String, Iterable<T>>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val isNull: Boolean = delegate == null

    override val fields: Iterable<String> = delegate!!.map { it.key }

    override val values: Iterable<T> = delegate!!.flatMap { it.value }

    override val entries: Iterable<Pair<String, T>> =
        delegate!!.entries.map { f -> f.value.map { v -> f.key to v } }.flatten()

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
internal class MapStructValueImpl<T : PartiQLValue>(
    private val delegate: Map<String, T>?,
    override val annotations: PersistentList<String>,
) : StructValue<T>() {

    override val isNull: Boolean = delegate == null

    override val fields: Iterable<String> = delegate!!.map { it.key }

    override val values: Iterable<T> = delegate!!.map { it.value }

    override val entries: Iterable<Pair<String, T>> = delegate!!.entries.map { it.key to it.value }

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

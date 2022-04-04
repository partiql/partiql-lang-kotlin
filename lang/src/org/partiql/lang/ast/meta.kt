/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

@file:Suppress("DEPRECATION") // We don't need warnings about ExprNode deprecation.

package org.partiql.lang.ast

import com.amazon.ion.IonWriter
import org.partiql.lang.util.IonWriterContext
import java.util.Arrays
import java.util.TreeMap

/**
 * The [Meta] interface is implemented by classes that provide an object mapping view to AST meta nodes.
 * A "meta" is any arbitrary data that is to be associated with an AST node.
 *
 * One example of [Meta] is [SourceLocationMeta], but any type of data can decorate any AST node if
 * that data is contained within the fields of a class implementing [Meta].
 *
 * The primary reason this is needed is to allow an AST node's decorations to be serializable.
 *
 * @see MetaDeserializer
 * @see MetaContainer
 * @see HasMetas
 */
interface Meta {
    /** The tag which will be given to this meta during serialization. */
    val tag: String

    /** A flag indicating if we should attempt to serialize this meta or not. */
    val shouldSerialize: Boolean get() = true

    /**
     * Serializes the contents of the meta.
     *
     * Note that implementers should *not* serialize the tag as this is handled by the serializer.
     * Only the meta's contents should be serialized.
     */
    fun serialize(writer: IonWriter) {
        // The default implementation writes a null value.
        // This is suitable for those metas which do not have any properties (i.e. those metas which are used solely
        // as a "flag".
        writer.writeNull()
    }
}

/**
 * An interface that allows the creation of extension functions that operate on objects
 * that contain an instance of MetaContainer.
 */
interface HasMetas {
    val metas: MetaContainer
}

/**
 * An immutable container for [Meta] instances.
 */
@Deprecated("Please use com.amazon.ionelement.api.MetaContainer")
interface MetaContainer : Iterable<Meta> {

    /**
     * Creates a new instance of [MetaContainer] which includes all of the previous metas and the specified meta.
     * If the specified meta already exists it will be overwritten.
     */
    fun add(meta: Meta): MetaContainer

    /**
     * Returns if this container contains a meta with the specified [tagName].
     */
    fun hasMeta(tagName: String): Boolean

    /**
     * Returns the annotation with the specified type or throws an exception if it wasn't found.
     **/
    operator fun get(tagName: String): Meta

    /** Returns the meta with the specified type or null if it wasn't found. */
    fun find(tagName: String): Meta?

    /**
     * Returns true to indicate that this instance of [MetaContainer] contains one or more meta containers.
     */
    val shouldSerialize: Boolean

    /**
     * Writes each meta contained within to a "bag" style sexp node.
     * I.e. `((<key> <value>)...)`.
     *
     * Meta nodes should be sorted alphabetically using their tag names.
     */
    fun serialize(writer: IonWriter)
}

/**
 * Contains a node's meta objects..
 */
private data class MetaContainerImpl internal constructor(private val metas: TreeMap<String, Meta> = TreeMap()) : MetaContainer {
    override fun iterator(): Iterator<Meta> = metas.values.iterator()

    override fun add(meta: Meta): MetaContainer {
        return this + metaContainerOf(meta)
    }

    override fun hasMeta(tagName: String) =
        metas.containsKey(tagName)

    override operator fun get(tagName: String): Meta =
        metas[tagName] ?: throw IllegalArgumentException("Meta with tag '$tagName' is not present in this MetaContainer instance.")

    override fun find(tagName: String): Meta? = metas[tagName]

    override val shouldSerialize = this.metas.any { it.value.shouldSerialize }

    override fun serialize(writer: IonWriter) {
        IonWriterContext(writer).apply {
            struct {
                // Metas must be sorted by tag name--this is handled for us automatically by [TreeMap].
                metas.values
                    .filter { it.shouldSerialize }
                    .forEach {
                        this.setNextFieldName(it.tag)
                        it.serialize(writer)
                    }
            }
        }
    }

    /**
     * Generates a hashCode for this [MetaContainer].
     *
     * Note: this exists only because we override `equals` below.  [MetaContainer] instances are not something that
     * would normally be used as a key or unique value.
     */
    override fun hashCode(): Int =
        Arrays.hashCode((metas.keys.toList() + metas.values.toList()).toTypedArray())

    override fun equals(other: Any?): Boolean =
        when {
            this === other -> true
            else -> when (other) {
                null -> false
                is MetaContainerImpl -> {
                    when {
                        metas.size != other.metas.size -> false
                        else -> {
                            metas.forEach {
                                val otherValue = other.metas[it.key]
                                when (otherValue) {
                                    null -> return@equals false
                                    else -> {
                                        if (it.value != otherValue) {
                                            return@equals false
                                        }
                                    }
                                }
                            }
                            true
                        }
                    }
                }
                else -> false
            }
        }
}

/** Constructs a container with the specified metas. */
@Deprecated("Please use org.partiql.lang.domains.metaContainerOf")
fun metaContainerOf(vararg metas: Meta): MetaContainer = metaContainerOf(metas.asIterable())

/** Empty meta container. */
@Deprecated("Please use com.amazon.ionelement.api.emptyMetaContainer")
val emptyMetaContainer: MetaContainer = metaContainerOf()

/**
 * Constructs a container with the elements found within [metas].
 */
@Deprecated("Please use com.amazon.ionelement.api.metaContainerOf")
fun metaContainerOf(metas: Iterable<Meta>): MetaContainer {
    return MetaContainerImpl(
        TreeMap<String, Meta>().apply {
            metas.forEach {
                // Sanity check to make sure there are no duplicate keys (the type of the Meta instance is used as the key)
                if (containsKey(it.tag)) {
                    IllegalArgumentException("List of metas contains one or more duplicate s-expression tag: ${it.tag}")
                }
                put(it.tag, it)
            }
        }
    )
}

infix fun Class<*>.to(m: Meta) = Pair(this, m)

/**
 * Merges two meta containers.
 *
 * Entries with keys that exist in `this` will be overwritten by any that exist in [other].
 */
operator fun MetaContainer.plus(other: MetaContainer): MetaContainer =
    MetaContainerImpl(
        TreeMap<String, Meta>().also { newMap ->
            forEach { newMap.put(it.tag, it) }
            other.forEach { newMap.put(it.tag, it) }
        }
    )

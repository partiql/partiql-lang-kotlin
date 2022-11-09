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

package org.partiql.lang.ast

import com.amazon.ion.IonWriter
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.plus
import org.partiql.lang.domains.metaContainerOf

/**
 * The [Meta] interface is implemented by classes that provide an object mapping view to AST meta nodes.
 * A "meta" is any arbitrary data that is to be associated with an AST node.
 *
 * One example of [Meta] is [SourceLocationMeta], but any type of data can decorate any AST node if
 * that data is contained within the fields of a class implementing [Meta].
 *
 * The primary reason this is needed is to allow an AST node's decorations to be serializable.
 *
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

infix fun Class<*>.to(m: Meta) = Pair(this, m)

fun MetaContainer.find(tagName: String): Meta? = this[tagName] as Meta?

fun MetaContainer.hasMeta(tagName: String) = this.containsKey(tagName)

fun MetaContainer.add(meta: Meta): MetaContainer = this.plus(metaContainerOf(meta))

internal typealias IonElementMetaContainer = com.amazon.ionelement.api.MetaContainer

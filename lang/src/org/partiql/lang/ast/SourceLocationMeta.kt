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

import com.amazon.ion.*
import com.amazon.ionelement.api.metaOrNull
import org.partiql.lang.util.*

/**
 * Represents a specific location within a source file.
 */
data class SourceLocationMeta(val lineNum: Long, val charOffset: Long, val length: Long = -1) : Meta {
    override fun toString() = "$lineNum:$charOffset:${if(length > 0) length.toString() else "<unknown>"}"

    override val tag = TAG



    override fun serialize(writer: IonWriter) {
        IonWriterContext(writer).apply {
            struct {
                int("line_num", lineNum)
                int("char_offset", charOffset)
                int("length", length)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceLocationMeta) return false

        if (lineNum != other.lineNum) return false
        if (charOffset != other.charOffset) return false

        // if length is unknown or the other is unknown, ignore the length.
        if(length > 0 && other.length > 0 && length != other.length) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lineNum.hashCode()
        result = 31 * result + charOffset.hashCode()

        // if the length is unknown, ignore it.
        if(length > 0) {
            result = 31 * result + length.hashCode()
        }
        return result
    }

    companion object {
        const val TAG = "\$source_location"
        val deserializer = object : MetaDeserializer {
            override val tag = TAG
            override fun deserialize(sexp: IonSexp): Meta {
                val struct = sexp.first().asIonStruct()
                val lineNum = struct.field("line_num").longValue()
                val charOffset = struct.field("char_offset").longValue()
                val length = struct.field("length").longValue()

                return SourceLocationMeta(lineNum, charOffset, length)
            }
        }
    }
}

val PartiQlMetaContainer.sourceLocation: SourceLocationMeta? get() = find(SourceLocationMeta.TAG) as SourceLocationMeta?
val IonElementMetaContainer.sourceLocation: SourceLocationMeta? get() = metaOrNull(SourceLocationMeta.TAG) as SourceLocationMeta?

// TODO generalize this better against MetaContainer

/** Retrieves the source locations as a container. */
val PartiQlMetaContainer.sourceLocationContainer: PartiQlMetaContainer
    get() = sourceLocation?.let {
        metaContainerOf(it)
    } ?: emptyMetaContainer
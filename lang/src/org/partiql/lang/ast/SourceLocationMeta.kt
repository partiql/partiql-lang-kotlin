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
import org.partiql.lang.util.*

/**
 * Represents a specific location within a source file.
 */
data class SourceLocationMeta(val lineNum: Long, val charOffset: Long) : Meta {
    override fun toString() = "$lineNum:$charOffset"

    override val tag = TAG

    override fun serialize(writer: IonWriter) {
        IonWriterContext(writer).apply {
            struct {
                int("line_num", lineNum)
                int("char_offset", charOffset)
            }
        }
    }

    companion object {
        const val TAG = "\$source_location"
        val deserializer = object : MetaDeserializer {
            override val tag = TAG
            override fun deserialize(sexp: IonSexp): Meta {
                val struct = sexp.first().asIonStruct()
                val lineNum = struct.field("line_num").longValue()
                val charOffset = struct.field("char_offset").longValue()

                return SourceLocationMeta(lineNum, charOffset)
            }
        }
    }
}

val MetaContainer.sourceLocation: SourceLocationMeta? get() = find(SourceLocationMeta.TAG) as SourceLocationMeta?


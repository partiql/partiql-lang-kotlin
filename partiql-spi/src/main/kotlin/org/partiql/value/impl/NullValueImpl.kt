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
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.bagValue
import org.partiql.value.binaryValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.byteValue
import org.partiql.value.charValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.intervalValue
import org.partiql.value.listValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import org.partiql.value.util.PartiQLValueVisitor

@OptIn(PartiQLValueExperimental::class)
internal data class NullValueImpl(
    override val annotations: PersistentList<String>,
) : NullValue() {
    override fun withType(type: PartiQLValueType): PartiQLValue = when (type) {
        PartiQLValueType.ANY -> this
        PartiQLValueType.BOOL -> boolValue(null, annotations)
        PartiQLValueType.INT8 -> int8Value(null, annotations)
        PartiQLValueType.INT16 -> int16Value(null, annotations)
        PartiQLValueType.INT32 -> int32Value(null, annotations)
        PartiQLValueType.INT64 -> int64Value(null, annotations)
        PartiQLValueType.INT -> intValue(null, annotations)
        PartiQLValueType.DECIMAL -> decimalValue(null, annotations)
        PartiQLValueType.DECIMAL_ARBITRARY -> decimalValue(null, annotations)
        PartiQLValueType.FLOAT32 -> float32Value(null, annotations)
        PartiQLValueType.FLOAT64 -> float64Value(null, annotations)
        PartiQLValueType.CHAR -> charValue(null, annotations)
        PartiQLValueType.STRING -> stringValue(null, annotations)
        PartiQLValueType.SYMBOL -> symbolValue(null, annotations)
        PartiQLValueType.BINARY -> binaryValue(null, annotations)
        PartiQLValueType.BYTE -> byteValue(null, annotations)
        PartiQLValueType.BLOB -> blobValue(null, annotations)
        PartiQLValueType.CLOB -> clobValue(null, annotations)
        PartiQLValueType.DATE -> dateValue(null, annotations)
        PartiQLValueType.TIME -> timeValue(null, annotations)
        PartiQLValueType.TIMESTAMP -> timestampValue(null, annotations)
        PartiQLValueType.INTERVAL -> intervalValue(null, annotations)
        PartiQLValueType.BAG -> bagValue<PartiQLValue>(null, annotations)
        PartiQLValueType.LIST -> listValue<PartiQLValue>(null, annotations)
        PartiQLValueType.SEXP -> sexpValue<PartiQLValue>(null, annotations)
        PartiQLValueType.STRUCT -> structValue<PartiQLValue>(null, annotations)
        PartiQLValueType.NULL -> this
        PartiQLValueType.MISSING -> error("cast to missing not supported")
    }

    override fun copy(annotations: Annotations) = NullValueImpl(annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): NullValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): NullValue = _withoutAnnotations()

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitNull(this, ctx)
}

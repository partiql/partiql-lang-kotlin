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
import org.partiql.errors.DataException
import org.partiql.value.Annotations
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigDecimal

internal data class Float64ValueImpl(
    override val value: Double?,
    override val annotations: PersistentList<String>,
) : Float64Value() {
    override fun copy(annotations: Annotations) = Float64ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Float64Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Float64Value = _withoutAnnotations()
    override fun toInt8(): Int8Value {
        if (this.value == null) {
            return int8Value(null, annotations)
        }
        if (this.value > Byte.MAX_VALUE || this.value < Byte.MIN_VALUE) {
            throw DataException("Overflow when casting ${this.value} to INT8")
        }
        return int8Value(this.value.toInt().toByte(), annotations)
    }

    override fun toInt16(): Int16Value {
        if (this.value == null) {
            return int16Value(null, annotations)
        }
        if (this.value > Short.MAX_VALUE || this.value < Short.MIN_VALUE) {
            throw DataException("Overflow when casting ${this.value} to INT16")
        }
        return int16Value(this.value.toInt().toShort(), annotations)
    }

    override fun toInt32(): Int32Value {
        if (this.value == null) {
            return int32Value(null, annotations)
        }
        if (this.value > Int.MAX_VALUE || this.value < Int.MIN_VALUE) {
            throw DataException("Overflow when casting ${this.value} to INT32")
        }
        return int32Value(this.value.toInt(), annotations)
    }

    override fun toInt64(): Int64Value {
        if (this.value == null) {
            return int64Value(null, annotations)
        }
        if (this.value > Long.MAX_VALUE || this.value < Long.MIN_VALUE) {
            throw DataException("Overflow when casting ${this.value} to INT64")
        }
        return int64Value(this.value.toLong(), annotations)
    }

    override fun toInt(): IntValue =
        intValue(this.value?.let { BigDecimal(it) }?.toBigInteger(), annotations)

    override fun toDecimal(): DecimalValue =
        decimalValue(this.value?.let { BigDecimal(it) }, annotations)

    override fun toFloat32(): Float32Value {
        if (this.value == null) {
            return float32Value(null, annotations)
        }
        if (this.value > Float.MAX_VALUE || this.value < Float.MIN_VALUE) {
            throw DataException("Overflow when casting ${this.value} to Float32")
        }
        return float32Value(this.value.toFloat(), annotations)
    }

    override fun toFloat64(): Float64Value = this

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitFloat64(this, ctx)
}

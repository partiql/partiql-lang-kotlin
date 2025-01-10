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
import org.partiql.spi.errors.DataException
import org.partiql.value.Annotations
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.datetime.DateTimeUtil.toBigDecimal
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigInteger

internal data class Int16ValueImpl(
    override val value: Short?,
    override val annotations: PersistentList<String>,
) : Int16Value() {

    override fun copy(annotations: Annotations) = Int16ValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): Int16Value = _withAnnotations(annotations)

    override fun withoutAnnotations(): Int16Value = _withoutAnnotations()
    override fun toInt8(): Int8Value {
        val byte = this.value?.toByte() ?: return int8Value(null, annotations)
        if (byte.toShort() != this.value) {
            throw DataException("Overflow when casting ${this.value} to INT8")
        }
        return int8Value(byte, annotations)
    }

    override fun toInt16(): Int16Value = this

    override fun toInt32(): Int32Value = int32Value(this.value?.toInt(), annotations)

    override fun toInt64(): Int64Value = int64Value(this.value?.toLong(), annotations)

    override fun toInt(): IntValue = intValue(this.value?.toLong()?.let { BigInteger.valueOf(it) }, annotations)

    override fun toDecimal(): DecimalValue = decimalValue(this.value?.toBigDecimal(), annotations)

    override fun toFloat32(): Float32Value = float32Value(this.value?.toFloat(), annotations)

    override fun toFloat64(): Float64Value = float64Value(this.value?.toDouble(), annotations)

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitInt16(this, ctx)
}

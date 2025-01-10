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
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.util.PartiQLValueVisitor
import java.math.BigDecimal
import java.math.RoundingMode

internal data class DecimalValueImpl(
    override val value: BigDecimal?,
    override val annotations: PersistentList<String>,
) : DecimalValue() {

    override fun copy(annotations: Annotations) = DecimalValueImpl(value, annotations.toPersistentList())

    override fun withAnnotations(annotations: Annotations): DecimalValue = _withAnnotations(annotations)

    override fun withoutAnnotations(): DecimalValue = _withoutAnnotations()

    // permits if no leading significant digits loss
    // rounding down for cast
    override fun toInt8(): Int8Value =
        try {
            int8Value(this.value?.setScale(0, RoundingMode.DOWN)?.byteValueExact(), annotations)
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting ${this.value} to INT8")
        }

    override fun toInt16(): Int16Value =
        try {
            int16Value(this.value?.setScale(0, RoundingMode.DOWN)?.shortValueExact(), annotations)
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting ${this.value} to INT16")
        }

    override fun toInt32(): Int32Value =
        try {
            int32Value(this.value?.setScale(0, RoundingMode.DOWN)?.intValueExact(), annotations)
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting ${this.value} to INT32")
        }

    override fun toInt64(): Int64Value =
        try {
            int64Value(this.value?.setScale(0, RoundingMode.DOWN)?.longValueExact(), annotations)
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting ${this.value} to INT64")
        }

    override fun toInt(): IntValue = intValue(this.value?.setScale(0, RoundingMode.DOWN)?.toBigInteger(), annotations)

    override fun toDecimal(): DecimalValue = this

    override fun toFloat32(): Float32Value {
        val float = this.value?.toFloat()
        if (float == Float.NEGATIVE_INFINITY || float == Float.NEGATIVE_INFINITY) {
            throw DataException("Overflow when casting ${this.value} to FLOAT32")
        }
        return float32Value(float, annotations)
    }

    override fun toFloat64(): Float64Value {
        val double = this.value?.toDouble()
        if (double == Double.NEGATIVE_INFINITY || double == Double.NEGATIVE_INFINITY) {
            throw DataException("Overflow when casting ${this.value} to FLOAT64")
        }
        return float64Value(double, annotations)
    }

    override fun <R, C> accept(visitor: PartiQLValueVisitor<R, C>, ctx: C): R = visitor.visitDecimal(this, ctx)
}

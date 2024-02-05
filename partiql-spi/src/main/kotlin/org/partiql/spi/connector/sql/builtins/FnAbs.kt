// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.check
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import kotlin.math.absoluteValue

// TODO: When negate a negative value, we need to consider overflow
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = INT8,
        parameters = listOf(FnParameter("value", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int8Value {
        val value = args[0].check<Int8Value>().value!!
        return if (value < 0) int8Value(value.times(-1).toByte()) else int8Value(value)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = INT16,
        parameters = listOf(FnParameter("value", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int16Value {
        val value = args[0].check<Int16Value>().value!!
        return if (value < 0) int16Value(value.times(-1).toShort()) else int16Value(value)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = INT32,
        parameters = listOf(FnParameter("value", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<Int32Value>().value!!
        return int32Value(value.absoluteValue)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = INT64,
        parameters = listOf(FnParameter("value", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int64Value {
        val value = args[0].check<Int64Value>().value!!
        return int64Value(value.absoluteValue)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__INT__INT : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): IntValue {
        val value = args[0].check<IntValue>().value!!
        return intValue(value.abs())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): DecimalValue {
        val value = args[0].check<DecimalValue>().value!!
        return decimalValue(value.abs())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Float32Value {
        val value = args[0].check<Float32Value>().value!!
        return float32Value(value.absoluteValue)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_ABS__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "abs",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Float64Value {
        val value = args[0].check<Float64Value>().value!!
        return float64Value(value.absoluteValue)
    }
}

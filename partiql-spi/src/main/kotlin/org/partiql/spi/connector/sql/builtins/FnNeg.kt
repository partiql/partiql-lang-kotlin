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

// TODO: Handle Overflow
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT8,
        parameters = listOf(FnParameter("value", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int8Value>().value!!
        return int8Value(value.times(-1).toByte())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT16,
        parameters = listOf(FnParameter("value", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int16Value>().value!!
        return int16Value(value.times(-1).toShort())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT32,
        parameters = listOf(FnParameter("value", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int32Value>().value!!
        return int32Value(value.times(-1))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT64,
        parameters = listOf(FnParameter("value", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int64Value>().value!!
        return int64Value(value.times(-1L))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__INT__INT : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<IntValue>().value!!
        return intValue(value.negate())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<DecimalValue>().value!!
        return decimalValue(value.negate())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Float32Value>().value!!
        return float32Value(value.times(-1))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_NEG__FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "neg",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Float64Value>().value!!
        return float64Value(value.times(-1))
    }
}

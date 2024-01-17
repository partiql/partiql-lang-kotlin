// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT8__INT8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT8,
        parameters = listOf(FunctionParameter("value", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int8Value {
        val value = args[0].check<Int8Value>().value!!
        return int8Value(value.times(-1).toByte())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT16__INT16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT16,
        parameters = listOf(FunctionParameter("value", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int16Value {
        val value = args[0].check<Int16Value>().value!!
        return int16Value(value.times(-1).toShort())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT32__INT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT32,
        parameters = listOf(FunctionParameter("value", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<Int32Value>().value!!
        return int32Value(value.times(-1))
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT64__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT64,
        parameters = listOf(FunctionParameter("value", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int64Value {
        val value = args[0].check<Int64Value>().value!!
        return int64Value(value.times(-1L))
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__INT__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = INT,
        parameters = listOf(FunctionParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): IntValue {
        val value = args[0].check<IntValue>().value!!
        return intValue(value.negate())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): DecimalValue {
        val value = args[0].check<DecimalValue>().value!!
        return decimalValue(value.negate())
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__FLOAT32__FLOAT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = FLOAT32,
        parameters = listOf(FunctionParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Float32Value {
        val value = args[0].check<Float32Value>().value!!
        return float32Value(value.times(-1))
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_NEG__FLOAT64__FLOAT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "neg",
        returns = FLOAT64,
        parameters = listOf(FunctionParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Float64Value {
        val value = args[0].check<Float64Value>().value!!
        return float64Value(value.times(-1))
    }
}
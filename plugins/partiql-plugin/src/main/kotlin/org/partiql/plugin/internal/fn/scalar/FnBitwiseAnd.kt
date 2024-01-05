// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.check
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import kotlin.experimental.and

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BITWISE_AND__INT8_INT8__INT8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "bitwise_and",
        returns = INT8,
        parameters = listOf(
            FunctionParameter("lhs", INT8),
            FunctionParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int8Value {
        val arg0 = args[0].check<Int8Value>().value!!
        val arg1 = args[1].check<Int8Value>().value!!
        return int8Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BITWISE_AND__INT16_INT16__INT16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "bitwise_and",
        returns = INT16,
        parameters = listOf(
            FunctionParameter("lhs", INT16),
            FunctionParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int16Value {
        val arg0 = args[0].check<Int16Value>().value!!
        val arg1 = args[1].check<Int16Value>().value!!
        return int16Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BITWISE_AND__INT32_INT32__INT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "bitwise_and",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("lhs", INT32),
            FunctionParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val arg0 = args[0].check<Int32Value>().value!!
        val arg1 = args[1].check<Int32Value>().value!!
        return int32Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BITWISE_AND__INT64_INT64__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "bitwise_and",
        returns = INT64,
        parameters = listOf(
            FunctionParameter("lhs", INT64),
            FunctionParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int64Value {
        val arg0 = args[0].check<Int64Value>().value!!
        val arg1 = args[1].check<Int64Value>().value!!
        return int64Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_BITWISE_AND__INT_INT__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "bitwise_and",
        returns = INT,
        parameters = listOf(
            FunctionParameter("lhs", INT),
            FunctionParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): IntValue {
        val arg0 = args[0].check<IntValue>().value!!
        val arg1 = args[1].check<IntValue>().value!!
        return intValue(arg0 and arg1)
    }
}

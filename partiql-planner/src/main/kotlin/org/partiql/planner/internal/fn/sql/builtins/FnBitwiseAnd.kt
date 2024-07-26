// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BITWISE_AND__INT8_INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT8,
        parameters = listOf(
            FnParameter("lhs", INT8),
            FnParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Int8Value>().value!!
        val arg1 = args[1].check<Int8Value>().value!!
        return int8Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BITWISE_AND__INT16_INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT16,
        parameters = listOf(
            FnParameter("lhs", INT16),
            FnParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Int16Value>().value!!
        val arg1 = args[1].check<Int16Value>().value!!
        return int16Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BITWISE_AND__INT32_INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT32,
        parameters = listOf(
            FnParameter("lhs", INT32),
            FnParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Int32Value>().value!!
        val arg1 = args[1].check<Int32Value>().value!!
        return int32Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BITWISE_AND__INT64_INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT64,
        parameters = listOf(
            FnParameter("lhs", INT64),
            FnParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Int64Value>().value!!
        val arg1 = args[1].check<Int64Value>().value!!
        return int64Value(arg0 and arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BITWISE_AND__INT_INT__INT : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<IntValue>().value!!
        val arg1 = args[1].check<IntValue>().value!!
        return intValue(arg0 and arg1)
    }
}

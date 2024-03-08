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
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.NUMERIC_ARBITRARY
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
internal object Fn_DIVIDE__INT8_INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "divide",
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
        return int8Value((arg0 / arg1).toByte())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__INT16_INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "divide",
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
        return int16Value((arg0 / arg1).toShort())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__INT32_INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "divide",
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
        return int32Value(arg0 / arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__INT64_INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "divide",
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
        return int64Value(arg0 / arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__INT_INT__INT : Fn {

    override val signature = FnSignature(
        name = "divide",
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
        return intValue(arg0 / arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "divide",
        returns = NUMERIC_ARBITRARY,
        parameters = listOf(
            FnParameter("lhs", NUMERIC_ARBITRARY),
            FnParameter("rhs", NUMERIC_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<DecimalValue>().value!!
        val arg1 = args[1].check<DecimalValue>().value!!
        return decimalValue(arg0 / arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__FLOAT32_FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "divide",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("lhs", FLOAT32),
            FnParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Float32Value>().value!!
        val arg1 = args[1].check<Float32Value>().value!!
        return float32Value(arg0 / arg1)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_DIVIDE__FLOAT64_FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "divide",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("lhs", FLOAT64),
            FnParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Float64Value>().value!!
        val arg1 = args[1].check<Float64Value>().value!!
        return float64Value(arg0 / arg1)
    }
}
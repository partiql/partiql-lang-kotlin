// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

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
import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.SMALLINT
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.BIGINT
import org.partiql.value.PartiQLValueType.TINYINT
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

internal object Fn_PLUS__TINYINT_TINYINT__TINYINT : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = TINYINT,
        parameters = listOf(
            FnParameter("lhs", TINYINT),
            FnParameter("rhs", TINYINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int8Value>().value!!
        val arg1 = args[1].check<Int8Value>().value!!
        return int8Value((arg0 + arg1).toByte())
    }
}


internal object Fn_PLUS__SMALLINT_SMALLINT__SMALLINT : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = SMALLINT,
        parameters = listOf(
            FnParameter("lhs", SMALLINT),
            FnParameter("rhs", SMALLINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int16Value>().value!!
        val arg1 = args[1].check<Int16Value>().value!!
        return int16Value((arg0 + arg1).toShort())
    }
}


internal object Fn_PLUS__INT_INT__INT : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int32Value>().value!!
        val arg1 = args[1].check<Int32Value>().value!!
        return int32Value(arg0 + arg1)
    }
}


internal object Fn_PLUS__BIGINT_BIGINT__BIGINT : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = BIGINT,
        parameters = listOf(
            FnParameter("lhs", BIGINT),
            FnParameter("rhs", BIGINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Int64Value>().value!!
        val arg1 = args[1].check<Int64Value>().value!!
        return int64Value(arg0 + arg1)
    }
}


internal object Fn_PLUS__NUMERIC_NUMERIC__INT : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<IntValue>().value!!
        val arg1 = args[1].check<IntValue>().value!!
        return intValue(arg0 + arg1)
    }
}


internal object Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FnParameter("lhs", DECIMAL_ARBITRARY),
            FnParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<DecimalValue>().value!!
        val arg1 = args[1].check<DecimalValue>().value!!
        return decimalValue(arg0 + arg1)
    }
}


internal object Fn_PLUS__FLOAT32_FLOAT32__FLOAT32 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = FLOAT32,
        parameters = listOf(
            FnParameter("lhs", FLOAT32),
            FnParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Float32Value>().value!!
        val arg1 = args[1].check<Float32Value>().value!!
        return float32Value(arg0 + arg1)
    }
}


internal object Fn_PLUS__FLOAT64_FLOAT64__FLOAT64 : Fn {

    override val signature = FnSignature(
        name = "plus",
        returns = FLOAT64,
        parameters = listOf(
            FnParameter("lhs", FLOAT64),
            FnParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].check<Float64Value>().value!!
        val arg1 = args[1].check<Float64Value>().value!!
        return float64Value(arg0 + arg1)
    }
}

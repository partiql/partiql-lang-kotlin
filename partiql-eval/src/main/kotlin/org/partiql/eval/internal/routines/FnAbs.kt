// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

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
import org.partiql.value.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.value.PType.Kind.FLOAT32
import org.partiql.value.PType.Kind.FLOAT64
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.SMALLINT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TINYINT
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

internal object Fn_ABS__TINYINT__TINYINT : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = TINYINT,
        parameters = listOf(FnParameter("value", TINYINT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int8Value {
        val value = args[0].check<Int8Value>().value!!
        return if (value < 0) int8Value(value.times(-1).toByte()) else int8Value(value)
    }
}


internal object Fn_ABS__SMALLINT__SMALLINT : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = SMALLINT,
        parameters = listOf(FnParameter("value", SMALLINT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int16Value {
        val value = args[0].check<Int16Value>().value!!
        return if (value < 0) int16Value(value.times(-1).toShort()) else int16Value(value)
    }
}


internal object Fn_ABS__INT__INT : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<Int32Value>().value!!
        return int32Value(value.absoluteValue)
    }
}


internal object Fn_ABS__BIGINT__BIGINT : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = BIGINT,
        parameters = listOf(FnParameter("value", BIGINT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int64Value {
        val value = args[0].check<Int64Value>().value!!
        return int64Value(value.absoluteValue)
    }
}


internal object Fn_ABS__NUMERIC__INT : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = INT,
        parameters = listOf(FnParameter("value", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): IntValue {
        val value = args[0].check<IntValue>().value!!
        return intValue(value.abs())
    }
}


internal object Fn_ABS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(FnParameter("value", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): DecimalValue {
        val value = args[0].check<DecimalValue>().value!!
        return decimalValue(value.abs())
    }
}


internal object Fn_ABS__FLOAT32__FLOAT32 : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = FLOAT32,
        parameters = listOf(FnParameter("value", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Float32Value {
        val value = args[0].check<Float32Value>().value!!
        return float32Value(value.absoluteValue)
    }
}


internal object Fn_ABS__FLOAT64__FLOAT64 : Routine {

    override val signature = FnSignature(
        name = "abs",
        returns = FLOAT64,
        parameters = listOf(FnParameter("value", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Float64Value {
        val value = args[0].check<Float64Value>().value!!
        return float64Value(value.absoluteValue)
    }
}

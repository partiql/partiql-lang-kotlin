// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn

import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.DateValue
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
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.TextValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__INT8_INT8_INT8__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT8),
            FnParameter("lower", INT8),
            FnParameter("upper", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int8Value>().value!!
        val lower = args[1].check<Int8Value>().value!!
        val upper = args[2].check<Int8Value>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__INT16_INT16_INT16__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT16),
            FnParameter("lower", INT16),
            FnParameter("upper", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int16Value>().value!!
        val lower = args[1].check<Int16Value>().value!!
        val upper = args[2].check<Int16Value>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__INT32_INT32_INT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT32),
            FnParameter("lower", INT32),
            FnParameter("upper", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int32Value>().value!!
        val lower = args[1].check<Int32Value>().value!!
        val upper = args[2].check<Int32Value>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__INT64_INT64_INT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT64),
            FnParameter("lower", INT64),
            FnParameter("upper", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Int64Value>().value!!
        val lower = args[1].check<Int64Value>().value!!
        val upper = args[2].check<Int64Value>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__INT_INT_INT__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("lower", INT),
            FnParameter("upper", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<IntValue>().value!!
        val lower = args[1].check<IntValue>().value!!
        val upper = args[2].check<IntValue>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("lower", DECIMAL_ARBITRARY),
            FnParameter("upper", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<DecimalValue>().value!!
        val lower = args[1].check<DecimalValue>().value!!
        val upper = args[2].check<DecimalValue>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("lower", FLOAT32),
            FnParameter("upper", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Float32Value>().value!!
        val lower = args[1].check<Float32Value>().value!!
        val upper = args[2].check<Float32Value>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("lower", FLOAT64),
            FnParameter("upper", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<Float64Value>().value!!
        val lower = args[1].check<Float64Value>().value!!
        val upper = args[2].check<Float64Value>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__STRING_STRING_STRING__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("lower", STRING),
            FnParameter("upper", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TextValue<String>>().value!!
        val lower = args[1].check<TextValue<String>>().value!!
        val upper = args[2].check<TextValue<String>>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("lower", SYMBOL),
            FnParameter("upper", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TextValue<String>>().value!!
        val lower = args[1].check<TextValue<String>>().value!!
        val upper = args[2].check<TextValue<String>>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("lower", CLOB),
            FnParameter("upper", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val lower = args[1].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val upper = args[2].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__DATE_DATE_DATE__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("lower", DATE),
            FnParameter("upper", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<DateValue>().value!!
        val lower = args[1].check<DateValue>().value!!
        val upper = args[2].check<DateValue>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__TIME_TIME_TIME__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("lower", TIME),
            FnParameter("upper", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TimeValue>().value!!
        val lower = args[1].check<TimeValue>().value!!
        val upper = args[2].check<TimeValue>().value!!
        return boolValue(value in lower..upper)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL : Fn {

    override val signature = FnSignature(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("lower", TIMESTAMP),
            FnParameter("upper", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<TimestampValue>().value!!
        val lower = args[1].check<TimestampValue>().value!!
        val upper = args[2].check<TimestampValue>().value!!
        return boolValue(value in lower..upper)
    }
}

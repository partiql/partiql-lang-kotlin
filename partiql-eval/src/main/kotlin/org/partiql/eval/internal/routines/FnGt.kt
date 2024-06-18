// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BoolValue
import org.partiql.value.DateValue
import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.PType.Kind.DATE
import org.partiql.value.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.value.PType.Kind.FLOAT32
import org.partiql.value.PType.Kind.FLOAT64
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.SMALLINT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TINYINT
import org.partiql.value.PType.Kind.STRING
import org.partiql.value.PType.Kind.SYMBOL
import org.partiql.value.PType.Kind.TIME
import org.partiql.value.PType.Kind.TIMESTAMP
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.boolValue
import org.partiql.value.check


internal object Fn_GT__TINYINT_TINYINT__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", TINYINT),
            FnParameter("rhs", TINYINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<Int8Value>()
        val rhs = args[1].check<Int8Value>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__SMALLINT_SMALLINT__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", SMALLINT),
            FnParameter("rhs", SMALLINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<Int16Value>()
        val rhs = args[1].check<Int16Value>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__INT_INT__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<Int32Value>()
        val rhs = args[1].check<Int32Value>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__BIGINT_BIGINT__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BIGINT),
            FnParameter("rhs", BIGINT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<Int64Value>()
        val rhs = args[1].check<Int64Value>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__NUMERIC_NUMERIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<IntValue>()
        val rhs = args[1].check<IntValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", DECIMAL_ARBITRARY),
            FnParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<DecimalValue>()
        val rhs = args[1].check<DecimalValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__FLOAT32_FLOAT32__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", FLOAT32),
            FnParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<Float32Value>()
        val rhs = args[1].check<Float32Value>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__FLOAT64_FLOAT64__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", FLOAT64),
            FnParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<Float64Value>()
        val rhs = args[1].check<Float64Value>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__STRING_STRING__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", STRING),
            FnParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<StringValue>()
        val rhs = args[1].check<StringValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__SYMBOL_SYMBOL__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", SYMBOL),
            FnParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<SymbolValue>()
        val rhs = args[1].check<SymbolValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__DATE_DATE__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", DATE),
            FnParameter("rhs", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<DateValue>()
        val rhs = args[1].check<DateValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__TIME_TIME__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", TIME),
            FnParameter("rhs", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<TimeValue>()
        val rhs = args[1].check<TimeValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__TIMESTAMP_TIMESTAMP__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", TIMESTAMP),
            FnParameter("rhs", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<TimestampValue>()
        val rhs = args[1].check<TimestampValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}


internal object Fn_GT__BOOL_BOOL__BOOL : Routine {

    override val signature = FnSignature(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val lhs = args[0].check<BoolValue>()
        val rhs = args[1].check<BoolValue>()
        return boolValue(lhs.value!! > rhs.value!!)
    }
}

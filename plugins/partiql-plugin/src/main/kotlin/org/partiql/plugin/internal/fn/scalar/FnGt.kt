// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
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
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__INT8_INT8__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT8),
            FunctionParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<Int8Value>()
        val r = args[1].check<Int8Value>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__INT16_INT16__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT16),
            FunctionParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<Int16Value>()
        val r = args[1].check<Int16Value>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__INT32_INT32__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT32),
            FunctionParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<Int32Value>()
        val r = args[1].check<Int32Value>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__INT64_INT64__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT64),
            FunctionParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<Int64Value>()
        val r = args[1].check<Int64Value>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__INT_INT__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT),
            FunctionParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<IntValue>()
        val r = args[1].check<IntValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL_ARBITRARY),
            FunctionParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<DecimalValue>()
        val r = args[1].check<DecimalValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__FLOAT32_FLOAT32__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT32),
            FunctionParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<Float32Value>()
        val r = args[1].check<Float32Value>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__FLOAT64_FLOAT64__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT64),
            FunctionParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<Float64Value>()
        val r = args[1].check<Float64Value>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__STRING_STRING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", STRING),
            FunctionParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<StringValue>()
        val r = args[1].check<StringValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__SYMBOL_SYMBOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", SYMBOL),
            FunctionParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<SymbolValue>()
        val r = args[1].check<SymbolValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__DATE_DATE__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DATE),
            FunctionParameter("rhs", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<DateValue>()
        val r = args[1].check<DateValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__TIME_TIME__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIME),
            FunctionParameter("rhs", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<TimeValue>()
        val r = args[1].check<TimeValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__TIMESTAMP_TIMESTAMP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIMESTAMP),
            FunctionParameter("rhs", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<TimestampValue>()
        val r = args[1].check<TimestampValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_GT__BOOL_BOOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "gt",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BOOL),
            FunctionParameter("rhs", BOOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val l = args[0].check<BoolValue>()
        val r = args[1].check<BoolValue>()
        return if (l.isNull || r.isNull) {
            boolValue(null)
        } else {
            boolValue(l.value!! > r.value!!)
        }
    }
}

// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.BagValue
import org.partiql.value.BinaryValue
import org.partiql.value.BlobValue
import org.partiql.value.BoolValue
import org.partiql.value.ByteValue
import org.partiql.value.CharValue
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
import org.partiql.value.IntervalValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BAG
import org.partiql.value.PartiQLValueType.BINARY
import org.partiql.value.PartiQLValueType.BLOB
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.BYTE
import org.partiql.value.PartiQLValueType.CHAR
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.DATE
import org.partiql.value.PartiQLValueType.DECIMAL
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.PartiQLValueType.INTERVAL
import org.partiql.value.PartiQLValueType.LIST
import org.partiql.value.PartiQLValueType.MISSING
import org.partiql.value.PartiQLValueType.NULL
import org.partiql.value.PartiQLValueType.SEXP
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.STRUCT
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.PartiQLValueType.TIME
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__ANY_ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", ANY),
            FunctionParameter("rhs", ANY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO ANY, ANY equals not clearly defined at the moment.
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0]
        val rhs = args[1]
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__BOOL_BOOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BOOL),
            FunctionParameter("rhs", BOOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BoolValue>()
        val rhs = args[1].check<BoolValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__INT8_INT8__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT8),
            FunctionParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int8Value>()
        val rhs = args[1].check<Int8Value>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__INT16_INT16__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT16),
            FunctionParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int16Value>()
        val rhs = args[1].check<Int16Value>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__INT32_INT32__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT32),
            FunctionParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int32Value>()
        val rhs = args[1].check<Int32Value>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__INT64_INT64__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT64),
            FunctionParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int64Value>()
        val rhs = args[1].check<Int64Value>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__INT_INT__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INT),
            FunctionParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<IntValue>()
        val rhs = args[1].check<IntValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__DECIMAL_DECIMAL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL),
            FunctionParameter("rhs", DECIMAL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<DecimalValue>()
        val rhs = args[1].check<DecimalValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL_ARBITRARY),
            FunctionParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<DecimalValue>()
        val rhs = args[1].check<DecimalValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__FLOAT32_FLOAT32__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT32),
            FunctionParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Float32Value>()
        val rhs = args[1].check<Float32Value>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__FLOAT64_FLOAT64__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT64),
            FunctionParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Float64Value>()
        val rhs = args[1].check<Float64Value>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__CHAR_CHAR__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", CHAR),
            FunctionParameter("rhs", CHAR),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<CharValue>()
        val rhs = args[1].check<CharValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__STRING_STRING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", STRING),
            FunctionParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<StringValue>()
        val rhs = args[1].check<StringValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__SYMBOL_SYMBOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", SYMBOL),
            FunctionParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<SymbolValue>()
        val rhs = args[1].check<SymbolValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__BINARY_BINARY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BINARY),
            FunctionParameter("rhs", BINARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BinaryValue>()
        val rhs = args[1].check<BinaryValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__BYTE_BYTE__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BYTE),
            FunctionParameter("rhs", BYTE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<ByteValue>()
        val rhs = args[1].check<ByteValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__BLOB_BLOB__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BLOB),
            FunctionParameter("rhs", BLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BlobValue>()
        val rhs = args[1].check<BlobValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__CLOB_CLOB__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", CLOB),
            FunctionParameter("rhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<ClobValue>()
        val rhs = args[1].check<ClobValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__DATE_DATE__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", DATE),
            FunctionParameter("rhs", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<DateValue>()
        val rhs = args[1].check<DateValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__TIME_TIME__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIME),
            FunctionParameter("rhs", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<TimeValue>()
        val rhs = args[1].check<TimeValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__TIMESTAMP_TIMESTAMP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", TIMESTAMP),
            FunctionParameter("rhs", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<TimestampValue>()
        val rhs = args[1].check<TimestampValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__INTERVAL_INTERVAL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", INTERVAL),
            FunctionParameter("rhs", INTERVAL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<IntervalValue>()
        val rhs = args[1].check<IntervalValue>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__BAG_BAG__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", BAG),
            FunctionParameter("rhs", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BagValue<*>>()
        val rhs = args[1].check<BagValue<*>>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__LIST_LIST__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", LIST),
            FunctionParameter("rhs", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<ListValue<*>>()
        val rhs = args[1].check<ListValue<*>>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__SEXP_SEXP__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", SEXP),
            FunctionParameter("rhs", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<SexpValue<*>>()
        val rhs = args[1].check<SexpValue<*>>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__STRUCT_STRUCT__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", STRUCT),
            FunctionParameter("rhs", STRUCT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<StructValue<*>>()
        val rhs = args[1].check<StructValue<*>>()
        return if (lhs.isNull || rhs.isNull) {
            boolValue(null)
        } else {
            boolValue(lhs == rhs)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__NULL_NULL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", NULL),
            FunctionParameter("rhs", NULL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO how does null comparison work? ie null.null == null.null or int8.null == null.null ??
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0]
        val rhs = args[1]
        return boolValue(lhs.isNull == rhs.isNull)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_EQ__MISSING_MISSING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("lhs", MISSING),
            FunctionParameter("rhs", MISSING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO how does `=` work with MISSING? As of now, always false.
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(false)
    }
}

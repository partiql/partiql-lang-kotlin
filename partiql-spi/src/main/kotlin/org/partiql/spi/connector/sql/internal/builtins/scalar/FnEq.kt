// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__ANY_ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", ANY),
            FnParameter("rhs", ANY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO ANY, ANY equals not clearly defined at the moment.
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0]
        val rhs = args[1]
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__BOOL_BOOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BOOL),
            FnParameter("rhs", BOOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BoolValue>()
        val rhs = args[1].check<BoolValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__INT8_INT8__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT8),
            FnParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int8Value>()
        val rhs = args[1].check<Int8Value>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__INT16_INT16__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT16),
            FnParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int16Value>()
        val rhs = args[1].check<Int16Value>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__INT32_INT32__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT32),
            FnParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int32Value>()
        val rhs = args[1].check<Int32Value>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__INT64_INT64__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT64),
            FnParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Int64Value>()
        val rhs = args[1].check<Int64Value>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__INT_INT__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INT),
            FnParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<IntValue>()
        val rhs = args[1].check<IntValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__DECIMAL_DECIMAL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", DECIMAL),
            FnParameter("rhs", DECIMAL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<DecimalValue>()
        val rhs = args[1].check<DecimalValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", DECIMAL_ARBITRARY),
            FnParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<DecimalValue>()
        val rhs = args[1].check<DecimalValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__FLOAT32_FLOAT32__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", FLOAT32),
            FnParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Float32Value>()
        val rhs = args[1].check<Float32Value>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__FLOAT64_FLOAT64__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", FLOAT64),
            FnParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<Float64Value>()
        val rhs = args[1].check<Float64Value>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__CHAR_CHAR__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", CHAR),
            FnParameter("rhs", CHAR),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<CharValue>()
        val rhs = args[1].check<CharValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__STRING_STRING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", STRING),
            FnParameter("rhs", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<StringValue>()
        val rhs = args[1].check<StringValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__SYMBOL_SYMBOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", SYMBOL),
            FnParameter("rhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<SymbolValue>()
        val rhs = args[1].check<SymbolValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__BINARY_BINARY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BINARY),
            FnParameter("rhs", BINARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BinaryValue>()
        val rhs = args[1].check<BinaryValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__BYTE_BYTE__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BYTE),
            FnParameter("rhs", BYTE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<ByteValue>()
        val rhs = args[1].check<ByteValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__BLOB_BLOB__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BLOB),
            FnParameter("rhs", BLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BlobValue>()
        val rhs = args[1].check<BlobValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__CLOB_CLOB__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", CLOB),
            FnParameter("rhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<ClobValue>()
        val rhs = args[1].check<ClobValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__DATE_DATE__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", DATE),
            FnParameter("rhs", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<DateValue>()
        val rhs = args[1].check<DateValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__TIME_TIME__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", TIME),
            FnParameter("rhs", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<TimeValue>()
        val rhs = args[1].check<TimeValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__TIMESTAMP_TIMESTAMP__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", TIMESTAMP),
            FnParameter("rhs", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<TimestampValue>()
        val rhs = args[1].check<TimestampValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__INTERVAL_INTERVAL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", INTERVAL),
            FnParameter("rhs", INTERVAL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<IntervalValue>()
        val rhs = args[1].check<IntervalValue>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__BAG_BAG__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", BAG),
            FnParameter("rhs", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<BagValue<*>>()
        val rhs = args[1].check<BagValue<*>>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__LIST_LIST__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", LIST),
            FnParameter("rhs", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<ListValue<*>>()
        val rhs = args[1].check<ListValue<*>>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__SEXP_SEXP__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", SEXP),
            FnParameter("rhs", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<SexpValue<*>>()
        val rhs = args[1].check<SexpValue<*>>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__STRUCT_STRUCT__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", STRUCT),
            FnParameter("rhs", STRUCT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val lhs = args[0].check<StructValue<*>>()
        val rhs = args[1].check<StructValue<*>>()
        return boolValue(lhs == rhs)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__NULL_NULL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", NULL),
            FnParameter("rhs", NULL),
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_EQ__MISSING_MISSING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(
            FnParameter("lhs", MISSING),
            FnParameter("rhs", MISSING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO how does `=` work with MISSING? As of now, always false.
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(false)
    }
}

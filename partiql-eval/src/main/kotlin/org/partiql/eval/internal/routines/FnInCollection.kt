// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
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
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BAG
import org.partiql.value.PType.Kind.BINARY
import org.partiql.value.PType.Kind.BLOB
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.PType.Kind.BYTE
import org.partiql.value.PType.Kind.CHAR
import org.partiql.value.PType.Kind.CLOB
import org.partiql.value.PType.Kind.DATE
import org.partiql.value.PType.Kind.DECIMAL
import org.partiql.value.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.value.PType.Kind.FLOAT32
import org.partiql.value.PType.Kind.FLOAT64
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.SMALLINT
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.BIGINT
import org.partiql.value.PType.Kind.TINYINT
import org.partiql.value.PType.Kind.INTERVAL
import org.partiql.value.PType.Kind.LIST
import org.partiql.value.PType.Kind.SEXP
import org.partiql.value.PType.Kind.STRING
import org.partiql.value.PType.Kind.STRUCT
import org.partiql.value.PType.Kind.SYMBOL
import org.partiql.value.PType.Kind.TIME
import org.partiql.value.PType.Kind.TIMESTAMP
import org.partiql.value.SexpValue
import org.partiql.value.StringValue
import org.partiql.value.StructValue
import org.partiql.value.SymbolValue
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.boolValue
import org.partiql.value.check


internal object Fn_IN_COLLECTION__DYNAMIC_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DYNAMIC_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DYNAMIC_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DYNAMIC),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BOOL_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BoolValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BOOL_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BoolValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BOOL_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BoolValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TINYINT_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TINYINT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int8Value>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TINYINT_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TINYINT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int8Value>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TINYINT_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TINYINT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int8Value>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SMALLINT_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SMALLINT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int16Value>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SMALLINT_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SMALLINT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int16Value>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SMALLINT_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SMALLINT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int16Value>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__INT_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int32Value>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__INT_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int32Value>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__INT_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int32Value>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BIGINT_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BIGINT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int64Value>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BIGINT_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BIGINT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int64Value>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BIGINT_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BIGINT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Int64Value>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__NUMERIC_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<IntValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__NUMERIC_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<IntValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__NUMERIC_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<IntValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DECIMAL_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DecimalValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DECIMAL_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DecimalValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DecimalValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DecimalValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DecimalValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DecimalValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__FLOAT32_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Float32Value>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__FLOAT32_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Float32Value>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Float32Value>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__FLOAT64_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Float64Value>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__FLOAT64_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Float64Value>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<Float64Value>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__CHAR_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CHAR),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<CharValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__CHAR_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CHAR),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<CharValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__CHAR_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CHAR),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<CharValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__STRING_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StringValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__STRING_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StringValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__STRING_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StringValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SYMBOL_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SymbolValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SYMBOL_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SymbolValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SymbolValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BINARY_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BINARY),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BinaryValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BINARY_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BINARY),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BinaryValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BINARY_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BINARY),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BinaryValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BYTE_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BYTE),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ByteValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BYTE_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BYTE),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ByteValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BYTE_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BYTE),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ByteValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BLOB_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BLOB),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BlobValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BLOB_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BLOB),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BlobValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BLOB_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BLOB),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BlobValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__CLOB_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ClobValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__CLOB_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ClobValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__CLOB_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ClobValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DATE_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DateValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DATE_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DateValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__DATE_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<DateValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TIME_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<TimeValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TIME_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<TimeValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TIME_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<TimeValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<TimestampValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<TimestampValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<TimestampValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__INTERVAL_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INTERVAL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<IntervalValue>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__INTERVAL_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INTERVAL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<IntervalValue>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__INTERVAL_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INTERVAL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<IntervalValue>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BAG_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BAG),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BagValue<Datum>>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BAG_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BAG),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BagValue<Datum>>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__BAG_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BAG),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<BagValue<Datum>>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__LIST_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", LIST),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ListValue<Datum>>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__LIST_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", LIST),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ListValue<Datum>>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__LIST_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", LIST),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<ListValue<Datum>>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SEXP_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SEXP),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SexpValue<Datum>>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SEXP_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SEXP),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SexpValue<Datum>>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__SEXP_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SEXP),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SexpValue<Datum>>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__STRUCT_BAG__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRUCT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StructValue<Datum>>()
        val collection = args[1].check<BagValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__STRUCT_LIST__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRUCT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StructValue<Datum>>()
        val collection = args[1].check<ListValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}


internal object Fn_IN_COLLECTION__STRUCT_SEXP__BOOL : Routine {

    override val signature = FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRUCT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StructValue<Datum>>()
        val collection = args[1].check<SexpValue<Datum>>()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return boolValue(true)
            }
        }
        return boolValue(false)
    }
}

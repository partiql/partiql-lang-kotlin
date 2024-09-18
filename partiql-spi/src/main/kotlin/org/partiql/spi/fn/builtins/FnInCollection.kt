// ktlint-disable filename
@file:Suppress("ClassName", "DEPRECATION")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_IN_COLLECTION__ANY_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__ANY_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__ANY_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.dynamic()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BOOL_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bool()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BOOL_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bool()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BOOL_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bool()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT8_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT8_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT8_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.tinyint()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT16_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT16_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT16_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.smallint()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT32_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.integer()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT32_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.integer()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT32_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.integer()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT64_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT64_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT64_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bigint()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.numeric()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.decimal()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.decimal()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.decimal()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("value", PType.decimal()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT32_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.real()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT32_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.real()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.real()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT64_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT64_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.doublePrecision()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CHAR_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.character(255)),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CHAR_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.character(255)),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CHAR_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.character(255)),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRING_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.string()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRING_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.string()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRING_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.string()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SYMBOL_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SYMBOL_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.symbol()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BLOB_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.blob(Int.MAX_VALUE)),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BLOB_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.blob(Int.MAX_VALUE)),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BLOB_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.blob(Int.MAX_VALUE)),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CLOB_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CLOB_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CLOB_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.clob(Int.MAX_VALUE)),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DATE_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.date()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DATE_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.date()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DATE_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.date()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIME_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.time(6)),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIME_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.time(6)),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIME_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.time(6)),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.timestamp(6)),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.timestamp(6)),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.timestamp(6)),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BAG_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bag()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BAG_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bag()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BAG_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.bag()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__LIST_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.array()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__LIST_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.array()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__LIST_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.array()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SEXP_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.sexp()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SEXP_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.sexp()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SEXP_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.sexp()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRUCT_BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.struct()),
            Parameter("collection", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRUCT_LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.struct()),
            Parameter("collection", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRUCT_SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("value", PType.struct()),
            Parameter("collection", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

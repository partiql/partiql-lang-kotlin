// ktlint-disable filename
@file:Suppress("ClassName", "DEPRECATION")
@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal object Fn_IN_COLLECTION__ANY_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__ANY_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__ANY_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.dynamic()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BOOL_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bool()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BOOL_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bool()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BOOL_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bool()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT8_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT8_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT8_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT16_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.smallint()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT16_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.smallint()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT16_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.smallint()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT32_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.integer()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT32_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.integer()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT32_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.integer()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT64_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bigint()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT64_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bigint()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT64_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bigint()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.numeric()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.numeric()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__INT_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.numeric()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.decimal()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.decimal()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.decimal()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.decimal()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.decimal()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.decimal()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT32_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.real()),
            FnParameter("collection", PType.bag()),
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
            @OptIn(PartiQLValueExperimental::class)
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT32_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.real()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.real()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT64_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.doublePrecision()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT64_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.doublePrecision()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.doublePrecision()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CHAR_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.character(255)),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CHAR_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.character(255)),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CHAR_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.character(255)),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRING_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.string()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRING_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.string()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRING_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.string()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SYMBOL_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.symbol()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SYMBOL_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.symbol()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.symbol()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BLOB_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.blob(Int.MAX_VALUE)),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BLOB_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.blob(Int.MAX_VALUE)),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BLOB_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.blob(Int.MAX_VALUE)),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CLOB_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.clob(Int.MAX_VALUE)),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CLOB_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.clob(Int.MAX_VALUE)),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__CLOB_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.clob(Int.MAX_VALUE)),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DATE_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.date()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DATE_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.date()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__DATE_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.date()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIME_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.time(6)),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIME_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.time(6)),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIME_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.time(6)),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.timestamp(6)),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.timestamp(6)),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.timestamp(6)),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BAG_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bag()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BAG_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bag()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__BAG_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.bag()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__LIST_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.array()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__LIST_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.array()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__LIST_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.array()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SEXP_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.typeSexp()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SEXP_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.typeSexp()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__SEXP_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.typeSexp()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRUCT_BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.struct()),
            FnParameter("collection", PType.bag()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRUCT_LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.struct()),
            FnParameter("collection", PType.array()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

internal object Fn_IN_COLLECTION__STRUCT_SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "in_collection",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("value", PType.struct()),
            FnParameter("collection", PType.typeSexp()),
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
            if (PartiQLValue.comparator().compare(value.toPartiQLValue(), v.toPartiQLValue()) == 0) {
                return Datum.bool(true)
            }
        }
        return Datum.bool(false)
    }
}

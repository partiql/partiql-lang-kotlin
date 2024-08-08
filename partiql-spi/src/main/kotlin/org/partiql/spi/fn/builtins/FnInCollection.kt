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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDynamic()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBool()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBool()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBool()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTinyInt()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSmallInt()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSmallInt()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSmallInt()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeInt()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeInt()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeInt()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBigInt()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBigInt()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBigInt()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeIntArbitrary()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeIntArbitrary()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeIntArbitrary()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDecimalArbitrary()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDecimalArbitrary()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDecimalArbitrary()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeDecimalArbitrary()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeDecimalArbitrary()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("value", PType.typeDecimalArbitrary()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeReal()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeReal()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeReal()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDoublePrecision()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDoublePrecision()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDoublePrecision()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeChar(255)),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeChar(255)),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeChar(255)),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeString()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSymbol()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSymbol()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSymbol()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBlob(Int.MAX_VALUE)),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBlob(Int.MAX_VALUE)),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBlob(Int.MAX_VALUE)),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeClob(Int.MAX_VALUE)),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeClob(Int.MAX_VALUE)),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeClob(Int.MAX_VALUE)),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDate()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDate()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeDate()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimeWithoutTZ(6)),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimeWithoutTZ(6)),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimeWithoutTZ(6)),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimestampWithoutTZ(6)),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimestampWithoutTZ(6)),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeTimestampWithoutTZ(6)),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBag()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBag()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeList()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeList()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSexp()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeSexp()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeStruct()),
            FnParameter("collection", PType.typeBag()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeStruct()),
            FnParameter("collection", PType.typeList()),
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
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("value", PType.typeStruct()),
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

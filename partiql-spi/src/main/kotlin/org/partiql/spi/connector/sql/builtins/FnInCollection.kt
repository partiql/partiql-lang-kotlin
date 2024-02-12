// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__ANY_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", ANY),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__ANY_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", ANY),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__ANY_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", ANY),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BOOL_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BOOL_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BOOL_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BOOL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT8_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT8),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT8_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT8),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT8_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT8),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT16_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT16),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT16_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT16),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT16_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT16),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT32_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT32),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT32_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT32),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT32_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT32),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT64_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT64),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT64_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT64),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT64_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT64),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INT_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DECIMAL_ARBITRARY_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT32_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT32_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT32_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT64_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT64_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__FLOAT64_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__CHAR_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CHAR),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__CHAR_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CHAR),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__CHAR_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CHAR),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__STRING_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__STRING_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__STRING_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__SYMBOL_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__SYMBOL_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__SYMBOL_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BINARY_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BINARY),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BINARY_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BINARY),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BINARY_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BINARY),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BYTE_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BYTE),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BYTE_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BYTE),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BYTE_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BYTE),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BLOB_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BLOB),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BLOB_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BLOB),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BLOB_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BLOB),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__CLOB_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__CLOB_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__CLOB_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DATE_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DATE_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__DATE_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__TIME_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__TIME_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__TIME_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__TIMESTAMP_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__TIMESTAMP_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__TIMESTAMP_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INTERVAL_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INTERVAL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INTERVAL_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INTERVAL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__INTERVAL_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INTERVAL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BAG_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BAG),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BAG_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BAG),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__BAG_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", BAG),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__LIST_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", LIST),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__LIST_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", LIST),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__LIST_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", LIST),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__SEXP_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SEXP),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__SEXP_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SEXP),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__SEXP_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SEXP),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__STRUCT_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRUCT),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__STRUCT_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRUCT),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__STRUCT_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRUCT),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__NULL_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", NULL),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__NULL_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", NULL),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__NULL_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", NULL),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__MISSING_BAG__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", MISSING),
            FnParameter("collection", BAG),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__MISSING_LIST__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", MISSING),
            FnParameter("collection", LIST),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IN_COLLECTION__MISSING_SEXP__BOOL : Fn {

    override val signature = object : FnSignature(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", MISSING),
            FnParameter("collection", SEXP),
        ),
        isNullCall = true,
        isNullable = false,
    ) {}
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

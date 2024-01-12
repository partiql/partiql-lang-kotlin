// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.*

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

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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
        TODO("Function eq not implemented")
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

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
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

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

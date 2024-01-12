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
internal object Fn_BETWEEN__INT8_INT8_INT8__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT8),
            FnParameter("lower", INT8),
            FnParameter("upper", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__INT16_INT16_INT16__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT16),
            FnParameter("lower", INT16),
            FnParameter("upper", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__INT32_INT32_INT32__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT32),
            FnParameter("lower", INT32),
            FnParameter("upper", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__INT64_INT64_INT64__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT64),
            FnParameter("lower", INT64),
            FnParameter("upper", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__INT_INT_INT__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", INT),
            FnParameter("lower", INT),
            FnParameter("upper", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DECIMAL_ARBITRARY),
            FnParameter("lower", DECIMAL_ARBITRARY),
            FnParameter("upper", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__FLOAT32_FLOAT32_FLOAT32__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT32),
            FnParameter("lower", FLOAT32),
            FnParameter("upper", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__FLOAT64_FLOAT64_FLOAT64__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", FLOAT64),
            FnParameter("lower", FLOAT64),
            FnParameter("upper", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__STRING_STRING_STRING__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("lower", STRING),
            FnParameter("upper", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__SYMBOL_SYMBOL_SYMBOL__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("lower", SYMBOL),
            FnParameter("upper", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__CLOB_CLOB_CLOB__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("lower", CLOB),
            FnParameter("upper", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__DATE_DATE_DATE__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", DATE),
            FnParameter("lower", DATE),
            FnParameter("upper", DATE),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__TIME_TIME_TIME__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIME),
            FnParameter("lower", TIME),
            FnParameter("upper", TIME),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_BETWEEN__TIMESTAMP_TIMESTAMP_TIMESTAMP__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "between",
        returns = BOOL,
        parameters = listOf(
            FnParameter("value", TIMESTAMP),
            FnParameter("lower", TIMESTAMP),
            FnParameter("upper", TIMESTAMP),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function between not implemented")
    }
}

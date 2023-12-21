package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", ANY), FunctionParameter("rhs", ANY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BOOL), FunctionParameter("rhs", BOOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT8), FunctionParameter("rhs", INT8)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT16), FunctionParameter("rhs", INT16)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT32), FunctionParameter("rhs", INT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT64), FunctionParameter("rhs", INT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INT), FunctionParameter("rhs", INT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DECIMAL), FunctionParameter("rhs", DECIMAL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DECIMAL_ARBITRARY), FunctionParameter("rhs", DECIMAL_ARBITRARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq9 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", FLOAT32), FunctionParameter("rhs", FLOAT32)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq10 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", FLOAT64), FunctionParameter("rhs", FLOAT64)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq11 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", CHAR), FunctionParameter("rhs", CHAR)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq12 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", STRING), FunctionParameter("rhs", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq13 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", SYMBOL), FunctionParameter("rhs", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq14 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BINARY), FunctionParameter("rhs", BINARY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq15 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BYTE), FunctionParameter("rhs", BYTE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BLOB), FunctionParameter("rhs", BLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq17 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", CLOB), FunctionParameter("rhs", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq18 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", DATE), FunctionParameter("rhs", DATE)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq19 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", TIME), FunctionParameter("rhs", TIME)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq20 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", TIMESTAMP), FunctionParameter("rhs", TIMESTAMP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq21 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", INTERVAL), FunctionParameter("rhs", INTERVAL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq22 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", BAG), FunctionParameter("rhs", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq23 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", LIST), FunctionParameter("rhs", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq24 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", SEXP), FunctionParameter("rhs", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq25 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", STRUCT), FunctionParameter("rhs", STRUCT)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq26 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", NULL), FunctionParameter("rhs", NULL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnEq27 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "eq",
        returns = BOOL,
        parameters = listOf(FunctionParameter("lhs", MISSING), FunctionParameter("rhs", MISSING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function eq not implemented")
    }
}

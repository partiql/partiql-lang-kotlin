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
internal object FnInCollection0 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection1 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection2 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection3 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection4 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection5 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BOOL), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection6 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT8), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection7 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT8), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT8), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection9 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT16), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection10 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT16), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection11 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT16), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection12 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT32), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection13 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT32), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection14 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT32), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection15 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT64), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT64), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection17 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT64), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection18 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection19 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection20 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INT), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection21 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DECIMAL), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection22 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DECIMAL), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection23 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DECIMAL), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection24 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection25 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection26 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DECIMAL_ARBITRARY), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection27 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", FLOAT32), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection28 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", FLOAT32), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection29 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", FLOAT32), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection30 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", FLOAT64), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection31 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", FLOAT64), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", FLOAT64), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection33 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CHAR), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection34 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CHAR), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection35 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CHAR), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection36 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRING), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection37 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRING), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection38 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRING), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection39 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SYMBOL), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection40 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SYMBOL), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection41 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SYMBOL), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection42 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BINARY), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection43 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BINARY), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection44 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BINARY), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection45 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BYTE), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection46 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BYTE), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection47 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BYTE), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection48 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BLOB), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection49 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BLOB), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection50 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BLOB), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection51 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CLOB), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection52 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CLOB), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection53 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", CLOB), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection54 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DATE), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection55 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DATE), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection56 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", DATE), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection57 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", TIME), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection58 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", TIME), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection59 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", TIME), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection60 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", TIMESTAMP), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection61 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", TIMESTAMP), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection62 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", TIMESTAMP), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection63 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INTERVAL), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INTERVAL), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection65 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", INTERVAL), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection66 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BAG), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection67 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BAG), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection68 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", BAG), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection69 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", LIST), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection70 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", LIST), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection71 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", LIST), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection72 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SEXP), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection73 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SEXP), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection74 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", SEXP), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection75 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRUCT), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection76 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRUCT), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection77 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", STRUCT), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection78 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", NULL), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection79 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", NULL), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection80 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", NULL), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection81 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", MISSING), FunctionParameter("collection", BAG)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection82 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", MISSING), FunctionParameter("collection", LIST)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object FnInCollection83 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "in_collection",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", MISSING), FunctionParameter("collection", SEXP)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function in_collection not implemented")
    }
}

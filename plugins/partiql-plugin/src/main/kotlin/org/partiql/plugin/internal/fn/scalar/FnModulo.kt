// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.DECIMAL_ARBITRARY
import org.partiql.value.PartiQLValueType.FLOAT32
import org.partiql.value.PartiQLValueType.FLOAT64
import org.partiql.value.PartiQLValueType.INT
import org.partiql.value.PartiQLValueType.INT16
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.INT8
import org.partiql.value.*
import org.partiql.value.PartiQLValueType.*
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__INT8_INT8__INT8 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = INT8,
        parameters = listOf(
            FunctionParameter("lhs", INT8),
            FunctionParameter("rhs", INT8),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): Int8Value = binaryOpInt8(args[0], args[1], Byte::mod)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__INT16_INT16__INT16 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = INT16,
        parameters = listOf(
            FunctionParameter("lhs", INT16),
            FunctionParameter("rhs", INT16),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): Int16Value = binaryOpInt16(args[0], args[1], Short::mod)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__INT32_INT32__INT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = INT32,
        parameters = listOf(
            FunctionParameter("lhs", INT32),
            FunctionParameter("rhs", INT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): Int32Value = binaryOpInt32(args[0], args[1], Int::mod)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__INT64_INT64__INT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = INT64,
        parameters = listOf(
            FunctionParameter("lhs", INT64),
            FunctionParameter("rhs", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): Int64Value = binaryOpInt64(args[0], args[1], Long::mod)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__INT_INT__INT : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = INT,
        parameters = listOf(
            FunctionParameter("lhs", INT),
            FunctionParameter("rhs", INT),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): IntValue = binaryOpInt(args[0], args[1], BigInteger::mod)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = DECIMAL_ARBITRARY,
        parameters = listOf(
            FunctionParameter("lhs", DECIMAL_ARBITRARY),
            FunctionParameter("rhs", DECIMAL_ARBITRARY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): DecimalValue = binaryOpDecimal(args[0], args[1], BigDecimal::remainder)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__FLOAT32_FLOAT32__FLOAT32 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = FLOAT32,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT32),
            FunctionParameter("rhs", FLOAT32),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): Float32Value = binaryOpFloat32(args[0], args[1], Float::mod)
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_MODULO__FLOAT64_FLOAT64__FLOAT64 : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "modulo",
        returns = FLOAT64,
        parameters = listOf(
            FunctionParameter("lhs", FLOAT64),
            FunctionParameter("rhs", FLOAT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    // TODO: This is untested and may be wrong. Java's mod operation does not match SQL's.
    override fun invoke(args: Array<PartiQLValue>): Float64Value = binaryOpFloat64(args[0], args[1], Double::mod)
}

// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import kotlin.math.absoluteValue

// TODO: When negate a negative value, we need to consider overflow
internal object Fn_ABS__INT8__INT8 : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.tinyint(),
        parameters = listOf(Parameter("value", PType.tinyint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        @Suppress("DEPRECATION")
        val value = args[0].byte
        return if (value < 0) Datum.tinyint(value.times(-1).toByte()) else Datum.tinyint(value)
    }
}

internal object Fn_ABS__INT16__INT16 : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.smallint(),
        parameters = listOf(Parameter("value", PType.smallint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].short
        return if (value < 0) Datum.smallint(value.times(-1).toShort()) else Datum.smallint(value)
    }
}

internal object Fn_ABS__INT32__INT32 : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.integer(),
        parameters = listOf(Parameter("value", PType.integer())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].int
        return Datum.integer(value.absoluteValue)
    }
}

internal object Fn_ABS__INT64__INT64 : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.bigint(),
        parameters = listOf(Parameter("value", PType.bigint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].long
        return Datum.bigint(value.absoluteValue)
    }
}

internal object Fn_ABS__INT__INT : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.numeric(),
        parameters = listOf(Parameter("value", PType.numeric())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigInteger
        return Datum.numeric(value.abs())
    }
}

internal object Fn_ABS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.decimal(),
        parameters = listOf(Parameter("value", PType.decimal())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigDecimal
        return Datum.decimal(value.abs())
    }
}

internal object Fn_ABS__FLOAT32__FLOAT32 : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.real(),
        parameters = listOf(Parameter("value", PType.real())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].float
        return Datum.real(value.absoluteValue)
    }
}

internal object Fn_ABS__FLOAT64__FLOAT64 : Function {

    override val signature = FnSignature(
        name = "abs",
        returns = PType.doublePrecision(),
        parameters = listOf(Parameter("value", PType.doublePrecision())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].double
        return Datum.doublePrecision(value.absoluteValue)
    }
}

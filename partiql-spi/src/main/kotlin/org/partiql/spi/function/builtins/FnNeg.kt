// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.types.PType

// TODO: Handle Overflow
internal object Fn_NEG__INT8__INT8 : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.tinyint(),
        parameters = listOf(Parameter("value", PType.tinyint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        @Suppress("DEPRECATION")
        val value = args[0].byte
        return Datum.tinyint(value.times(-1).toByte())
    }
}

internal object Fn_NEG__INT16__INT16 : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.smallint(),
        parameters = listOf(Parameter("value", PType.smallint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].short
        return Datum.smallint(value.times(-1).toShort())
    }
}

internal object Fn_NEG__INT32__INT32 : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.integer(),
        parameters = listOf(Parameter("value", PType.integer())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].int
        return Datum.integer(value.times(-1))
    }
}

internal object Fn_NEG__INT64__INT64 : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.bigint(),
        parameters = listOf(Parameter("value", PType.bigint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].long
        return Datum.bigint(value.times(-1L))
    }
}

internal object Fn_NEG__INT__INT : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.numeric(),
        parameters = listOf(Parameter("value", PType.numeric())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigInteger
        return Datum.numeric(value.negate())
    }
}

internal object Fn_NEG__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.decimal(),
        parameters = listOf(Parameter("value", PType.decimal())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].bigDecimal
        return Datum.decimal(value.negate())
    }
}

internal object Fn_NEG__FLOAT32__FLOAT32 : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.real(),
        parameters = listOf(Parameter("value", PType.real())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].float
        return Datum.real(value.times(-1))
    }
}

internal object Fn_NEG__FLOAT64__FLOAT64 : Function {

    override val signature = FnSignature(
        name = "neg",
        returns = PType.doublePrecision(),
        parameters = listOf(Parameter("value", PType.doublePrecision())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].double
        return Datum.doublePrecision(value.times(-1))
    }
}

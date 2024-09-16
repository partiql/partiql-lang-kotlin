// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.types.PType

internal object Fn_POS__INT8__INT8 : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.tinyint(),
        parameters = listOf(Parameter("value", PType.tinyint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT16__INT16 : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.smallint(),
        parameters = listOf(Parameter("value", PType.smallint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT32__INT32 : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.integer(),
        parameters = listOf(Parameter("value", PType.integer())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT64__INT64 : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.bigint(),
        parameters = listOf(Parameter("value", PType.bigint())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__INT__INT : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.numeric(),
        parameters = listOf(Parameter("value", PType.numeric())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__DECIMAL_ARBITRARY__DECIMAL_ARBITRARY : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.decimal(),
        parameters = listOf(Parameter("value", PType.decimal())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__FLOAT32__FLOAT32 : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.real(),
        parameters = listOf(Parameter("value", PType.real())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

internal object Fn_POS__FLOAT64__FLOAT64 : Function {

    override val signature = FnSignature(
        name = "pos",
        returns = PType.doublePrecision(),
        parameters = listOf(Parameter("value", PType.doublePrecision())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return args[0]
    }
}

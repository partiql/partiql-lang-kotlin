// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

// TODO: Handle Overflow
internal object Fn_PLUS__INT8_INT8__INT8 : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.tinyint(),
        parameters = listOf(
            Parameter("lhs", PType.tinyint()),
            Parameter("rhs", PType.tinyint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        @Suppress("DEPRECATION") val arg0 = args[0].byte
        @Suppress("DEPRECATION") val arg1 = args[1].byte
        return Datum.tinyint((arg0 + arg1).toByte())
    }
}

internal object Fn_PLUS__INT16_INT16__INT16 : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.smallint(),
        parameters = listOf(
            Parameter("lhs", PType.smallint()),
            Parameter("rhs", PType.smallint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].short
        val arg1 = args[1].short
        return Datum.smallint((arg0 + arg1).toShort())
    }
}

internal object Fn_PLUS__INT32_INT32__INT32 : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("lhs", PType.integer()),
            Parameter("rhs", PType.integer()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].int
        val arg1 = args[1].int
        return Datum.integer(arg0 + arg1)
    }
}

internal object Fn_PLUS__INT64_INT64__INT64 : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.bigint(),
        parameters = listOf(
            Parameter("lhs", PType.bigint()),
            Parameter("rhs", PType.bigint()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].long
        val arg1 = args[1].long
        return Datum.bigint(arg0 + arg1)
    }
}

internal object Fn_PLUS__INT_INT__INT : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") Parameter("rhs", PType.numeric()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bigInteger
        val arg1 = args[1].bigInteger
        return Datum.numeric(arg0 + arg1)
    }
}

internal object Fn_PLUS__DECIMAL_ARBITRARY_DECIMAL_ARBITRARY__DECIMAL_ARBITRARY :
    Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.decimal(),
        parameters = listOf(
            @Suppress("DEPRECATION") Parameter("lhs", PType.decimal()),
            @Suppress("DEPRECATION") Parameter("rhs", PType.decimal()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].bigDecimal
        val arg1 = args[1].bigDecimal
        return Datum.decimal(arg0 + arg1)
    }
}

internal object Fn_PLUS__FLOAT32_FLOAT32__FLOAT32 : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.real(),
        parameters = listOf(
            Parameter("lhs", PType.real()),
            Parameter("rhs", PType.real()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].float
        val arg1 = args[1].float
        return Datum.real(arg0 + arg1)
    }
}

internal object Fn_PLUS__FLOAT64_FLOAT64__FLOAT64 : Function {

    override val signature = FnSignature(
        name = "plus",
        returns = PType.doublePrecision(),
        parameters = listOf(
            Parameter("lhs", PType.doublePrecision()),
            Parameter("rhs", PType.doublePrecision()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val arg0 = args[0].double
        val arg1 = args[1].double
        return Datum.doublePrecision(arg0 + arg1)
    }
}

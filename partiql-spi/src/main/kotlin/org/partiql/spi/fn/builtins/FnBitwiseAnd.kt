// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import kotlin.experimental.and

internal object Fn_BITWISE_AND__INT8_INT8__INT8 : Function {

    override val signature = FnSignature(
        name = "bitwise_and",
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
        return Datum.tinyint(arg0 and arg1)
    }
}

internal object Fn_BITWISE_AND__INT16_INT16__INT16 : Function {

    override val signature = FnSignature(
        name = "bitwise_and",
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
        return Datum.smallint(arg0 and arg1)
    }
}

internal object Fn_BITWISE_AND__INT32_INT32__INT32 : Function {

    override val signature = FnSignature(
        name = "bitwise_and",
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
        return Datum.integer(arg0 and arg1)
    }
}

internal object Fn_BITWISE_AND__INT64_INT64__INT64 : Function {

    override val signature = FnSignature(
        name = "bitwise_and",
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
        return Datum.bigint(arg0 and arg1)
    }
}

internal object Fn_BITWISE_AND__INT_INT__INT : Function {

    override val signature = FnSignature(
        name = "bitwise_and",
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
        return Datum.numeric(arg0 and arg1)
    }
}

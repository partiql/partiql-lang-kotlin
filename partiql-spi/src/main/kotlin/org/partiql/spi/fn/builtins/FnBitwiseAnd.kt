// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import kotlin.experimental.and

internal object Fn_BITWISE_AND__INT8_INT8__INT8 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = PType.tinyint(),
        parameters = listOf(
            FnParameter("lhs", PType.tinyint()),
            FnParameter("rhs", PType.tinyint()),
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

internal object Fn_BITWISE_AND__INT16_INT16__INT16 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = PType.smallint(),
        parameters = listOf(
            FnParameter("lhs", PType.smallint()),
            FnParameter("rhs", PType.smallint()),
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

internal object Fn_BITWISE_AND__INT32_INT32__INT32 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = PType.integer(),
        parameters = listOf(
            FnParameter("lhs", PType.integer()),
            FnParameter("rhs", PType.integer()),
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

internal object Fn_BITWISE_AND__INT64_INT64__INT64 : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = PType.bigint(),
        parameters = listOf(
            FnParameter("lhs", PType.bigint()),
            FnParameter("rhs", PType.bigint()),
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

internal object Fn_BITWISE_AND__INT_INT__INT : Fn {

    override val signature = FnSignature(
        name = "bitwise_and",
        returns = PType.numeric(),
        parameters = listOf(
            @Suppress("DEPRECATION") FnParameter("lhs", PType.numeric()),
            @Suppress("DEPRECATION") FnParameter("rhs", PType.numeric()),
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

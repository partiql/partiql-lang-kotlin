package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_SIZE__BAG__INT32 : Function {

    override val signature = FnSignature(
        name = "size",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("container", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        return Datum.integer(container.count())
    }
}

internal object Fn_SIZE__LIST__INT32 : Function {

    override val signature = FnSignature(
        name = "size",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("container", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        return Datum.integer(container.count())
    }
}

internal object Fn_SIZE__SEXP__INT32 : Function {

    override val signature = FnSignature(
        name = "size",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("container", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        return Datum.integer(container.count())
    }
}

internal object Fn_SIZE__STRUCT__INT32 : Function {

    override val signature = FnSignature(
        name = "size",
        returns = PType.integer(),
        parameters = listOf(
            Parameter("container", PType.struct()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        var count = 0
        val iter = container.fields
        while (iter.hasNext()) {
            iter.next()
            count++
        }
        return Datum.integer(count)
    }
}

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_CARDINALITY__BAG__INT32 : Function {

    override val signature = FnSignature(
        name = "cardinality",
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

internal object Fn_CARDINALITY__LIST__INT32 : Function {

    override val signature = FnSignature(
        name = "cardinality",
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

internal object Fn_CARDINALITY__SEXP__INT32 : Function {

    override val signature = FnSignature(
        name = "cardinality",
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

internal object Fn_CARDINALITY__STRUCT__INT32 : Function {

    override val signature = FnSignature(
        name = "cardinality",
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
        val iter = container.fields.iterator()
        while (iter.hasNext()) {
            iter.next()
            count++
        }
        return Datum.integer(count)
    }
}

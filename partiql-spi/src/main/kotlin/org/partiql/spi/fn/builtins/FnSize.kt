package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_SIZE__BAG__INT32 : Fn {

    override val signature = FnSignature(
        name = "size",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("container", PType.typeBag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        return Datum.integer(container.count())
    }
}

internal object Fn_SIZE__LIST__INT32 : Fn {

    override val signature = FnSignature(
        name = "size",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("container", PType.typeList()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        return Datum.integer(container.count())
    }
}

internal object Fn_SIZE__SEXP__INT32 : Fn {

    override val signature = FnSignature(
        name = "size",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("container", PType.typeSexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        return Datum.integer(container.count())
    }
}

internal object Fn_SIZE__STRUCT__INT32 : Fn {

    override val signature = FnSignature(
        name = "size",
        returns = PType.typeInt(),
        parameters = listOf(
            FnParameter("container", PType.typeStruct()),
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

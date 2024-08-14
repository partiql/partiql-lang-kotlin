package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType

internal object Fn_EXISTS__BAG__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("container", PType.bag()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        val exists = container.iterator().hasNext()
        return Datum.bool(exists)
    }
}

internal object Fn_EXISTS__LIST__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("container", PType.array()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        val exists = container.iterator().hasNext()
        return Datum.bool(exists)
    }
}

internal object Fn_EXISTS__SEXP__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("container", PType.sexp()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        val exists = container.iterator().hasNext()
        return Datum.bool(exists)
    }
}

internal object Fn_EXISTS__STRUCT__BOOL : Fn {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            FnParameter("container", PType.struct()),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val container = args[0]
        val exists = container.fields.iterator().hasNext()
        return Datum.bool(exists)
    }
}

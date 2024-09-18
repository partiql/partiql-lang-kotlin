package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object Fn_EXISTS__BAG__BOOL : Function {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("container", PType.bag()),
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

internal object Fn_EXISTS__LIST__BOOL : Function {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("container", PType.array()),
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

internal object Fn_EXISTS__SEXP__BOOL : Function {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("container", PType.sexp()),
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

internal object Fn_EXISTS__STRUCT__BOOL : Function {

    override val signature = FnSignature(
        name = "exists",
        returns = PType.bool(),
        parameters = listOf(
            Parameter("container", PType.struct()),
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

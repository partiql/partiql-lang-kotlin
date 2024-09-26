package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_SIZE__BAG__INT32 = Function.static(

    name = "size",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.bag()),
    ),

) { args ->
    val container = args[0]
    Datum.integer(container.count())
}

internal val Fn_SIZE__LIST__INT32 = Function.static(

    name = "size",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.array()),
    ),

) { args ->
    val container = args[0]
    Datum.integer(container.count())
}

internal val Fn_SIZE__SEXP__INT32 = Function.static(

    name = "size",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.sexp()),
    ),

) { args ->
    val container = args[0]
    Datum.integer(container.count())
}

internal val Fn_SIZE__STRUCT__INT32 = Function.static(

    name = "size",
    returns = PType.integer(),
    parameters = arrayOf(
        Parameter("container", PType.struct()),
    ),

) { args ->
    val container = args[0]
    var count = 0
    val iter = container.fields
    while (iter.hasNext()) {
        iter.next()
        count++
    }
    Datum.integer(count)
}

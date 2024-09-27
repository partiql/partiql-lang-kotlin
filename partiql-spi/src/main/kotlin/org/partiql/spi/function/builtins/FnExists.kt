package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_EXISTS__BAG__BOOL = Function.static(

    name = "exists",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("container", PType.bag()),
    ),

) { args ->
    val container = args[0]
    val exists = container.iterator().hasNext()
    Datum.bool(exists)
}

internal val Fn_EXISTS__LIST__BOOL = Function.static(

    name = "exists",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("container", PType.array()),
    ),

) { args ->
    val container = args[0]
    val exists = container.iterator().hasNext()
    Datum.bool(exists)
}

internal val Fn_EXISTS__SEXP__BOOL = Function.static(

    name = "exists",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("container", PType.sexp()),
    ),

) { args ->
    val container = args[0]
    val exists = container.iterator().hasNext()
    Datum.bool(exists)
}

internal val Fn_EXISTS__STRUCT__BOOL = Function.static(

    name = "exists",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("container", PType.struct()),
    ),

) { args ->
    val container = args[0]
    val exists = container.fields.iterator().hasNext()
    Datum.bool(exists)
}

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

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

// TODO: Should this exist? EXISTS is meant to check that a subquery has at least one element in the resulting collection.
//  The `<exists predicate>` in SQL:1999 requires that the RHS be a syntactic `<table subquery>`,
//  which is always a BAG. Similarly, Section 8.9 references the cardinality of the RHS, which is an attribute of
//  collections. The term "degree" would be more characteristic of tuples.
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

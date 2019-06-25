package org.partiql.testscript.parser

internal sealed class Result<T>

internal class Success<T>(val value: T) : Result<T>()

internal class Error<T>(val errors: List<ParserError>) : Result<T>() {
    constructor(vararg errors: ParserError) : this(errors.toList())
}
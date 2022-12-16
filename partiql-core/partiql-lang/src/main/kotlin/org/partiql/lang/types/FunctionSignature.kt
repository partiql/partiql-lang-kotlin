/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.types

/**
 * A typed version of function signature.
 *
 * Parameters of function consists of single parameters (i.e non-variadic and mandatory) and
 * either an optional parameter or a variadic parameter.
 * A function cannot contain both optional and variadic parameters.
 */
class FunctionSignature private constructor (
    val name: String,
    val requiredParameters: List<StaticType>,
    val optionalParameter: StaticType? = null,
    val variadicParameter: VarargFormalParameter? = null,
    val returnType: StaticType,
    val unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
) {

    init {
        check(!(optionalParameter != null && variadicParameter != null)) {
            "Function '$name' contains both optional and variadic parameters."
        }
    }
    constructor(
        name: String,
        requiredParameters: List<StaticType>,
        optionalParameter: StaticType,
        returnType: StaticType,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, optionalParameter, null, returnType, unknownArguments)

    constructor(
        name: String,
        requiredParameters: List<StaticType>,
        variadicParameter: VarargFormalParameter,
        returnType: StaticType,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, null, variadicParameter, returnType, unknownArguments)

    constructor(
        name: String,
        requiredParameters: List<StaticType>,
        returnType: StaticType,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, null, null, returnType, unknownArguments)

    val arity: IntRange = let {
        val r = requiredParameters.size..requiredParameters.size
        val o = if (optionalParameter != null) 0..1 else 0..0
        val v = variadicParameter?.arityRange ?: 0..0

        (r.first + o.first + v.first)..when (v.last) {
            Int.MAX_VALUE -> Int.MAX_VALUE
            else -> (r.last + o.last + v.last)
        }
    }
}

/**
 * Indicates if a given function should allow unknown values to be propagated at evaluation time.
 */
enum class UnknownArguments {
    /**
     * Indicates that if an unknown argument (`NULL` or `MISSING`) is encountered, an unknown value depending
     * on the current [org.partiql.lang.eval.TypingMode]) should be returned without invoking the
     * function. Most functions will use this option to maintain consistent semantics with standard SQL.
     */
    PROPAGATE,

    /**
     * Indicates that unknown argument checking should be skipped and the function should always be invoked.
     *
     * Used for special functions like `COALESCE` and `ISNULL`, which are required to check their arguments
     * for known-ness.
     */
    PASS_THRU
}

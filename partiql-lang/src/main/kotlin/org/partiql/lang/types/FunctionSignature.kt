/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.types

import org.partiql.types.StaticType

/**
 * A typed version of function signature.
 *
 * Parameters of function consists of single parameters (i.e non-variadic and mandatory) and
 * either an optional parameter or a variadic parameter.
 * A function cannot contain both optional and variadic parameters.
 */
class FunctionSignature(
    val name: String,
    val requiredParameters: List<StaticType>,
    val returnType: StaticType,
    val unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
) {
    @Deprecated("This constructor is deprecated", level = DeprecationLevel.ERROR)
    constructor(
        name: String,
        requiredParameters: List<StaticType>,
        optionalParameter: StaticType,
        returnType: StaticType,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, returnType, unknownArguments)

    @Deprecated("This constructor is deprecated", level = DeprecationLevel.ERROR)
    constructor(
        name: String,
        requiredParameters: List<StaticType>,
        variadicParameter: VarargFormalParameter,
        returnType: StaticType,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, returnType, unknownArguments)

    @Deprecated("This constructor is deprecated", level = DeprecationLevel.ERROR)
    constructor(
        name: String,
        requiredParameters: List<StaticType>,
        optionalParameter: StaticType,
        variadicParameter: VarargFormalParameter,
        returnType: StaticType,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, returnType, unknownArguments)

    val arity: IntRange = requiredParameters.size..requiredParameters.size
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

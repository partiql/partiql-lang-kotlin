package org.partiql.lang.ots_work.interfaces.function

import org.partiql.lang.ots_work.interfaces.UnionOfScalarTypes

class FunctionSignature private constructor (
    val name: String,
    val requiredParameters: List<UnionOfScalarTypes>,
    val optionalParameter: UnionOfScalarTypes? = null,
    val variadicParameter: VarargFormalParameter? = null,
    val returnType: UnionOfScalarTypes,
    val unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
) {

    init {
        check(!(optionalParameter != null && variadicParameter != null)) {
            "Function '$name' contains both optional and variadic parameters."
        }
    }
    constructor(
        name: String,
        requiredParameters: List<UnionOfScalarTypes>,
        optionalParameter: UnionOfScalarTypes,
        returnType: UnionOfScalarTypes,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, optionalParameter, null, returnType, unknownArguments)

    constructor(
        name: String,
        requiredParameters: List<UnionOfScalarTypes>,
        variadicParameter: VarargFormalParameter,
        returnType: UnionOfScalarTypes,
        unknownArguments: UnknownArguments = UnknownArguments.PROPAGATE
    ) : this(name, requiredParameters, null, variadicParameter, returnType, unknownArguments)

    constructor(
        name: String,
        requiredParameters: List<UnionOfScalarTypes>,
        returnType: UnionOfScalarTypes,
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

// TODO: remove this class and use [TypingMode] to replace it
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

/**
 * Represents a variable number of arguments function parameter. Varargs are monomorpic, i.e. all elements are of
 * the **same** type
 */
data class VarargFormalParameter(
    val type: UnionOfScalarTypes,
    val arityRange: IntRange
) {
    constructor(type: UnionOfScalarTypes, minCount: Int) : this(type, minCount..Int.MAX_VALUE)
    override fun toString(): String = "$type..."
}

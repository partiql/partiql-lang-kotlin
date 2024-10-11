package org.partiql.eval.internal

/**
 * This class holds the evaluation environment.
 */
internal class Environment private constructor(
    @JvmField public val parameters: Parameters,
    @JvmField public val scope: Scope,
) {

    constructor() : this(Parameters.EMPTY, Scope.empty)

    constructor(parameters: Parameters) : this(parameters, Scope.empty)

    /**
     * TODO make push(scope) and use pop() to avoid extra instantiations.
     */
    public fun push(record: Record): Environment = Environment(parameters, scope.push(record))

    override fun toString(): String = scope.toString()
}

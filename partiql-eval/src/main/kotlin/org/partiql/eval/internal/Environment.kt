package org.partiql.eval.internal

import org.partiql.eval.operator.Record

/**
 * This class holds the evaluation environment.
 */
internal class Environment private constructor(
    @JvmField val parameters: Parameters,
    @JvmField val scope: Scope,
) {

    /**
     * Default constructor with no parameters.
     */
    constructor() : this(Parameters.EMPTY, Scope.empty)

    /**
     * Default constructor with parameters.
     */
    constructor(parameters: Parameters) : this(parameters, Scope.empty)

    /**
     * TODO make push(scope) and use pop() to avoid extra instantiations.
     */
    fun push(record: Record): Environment = Environment(parameters, scope.push(record))

    override fun toString(): String = scope.toString()
}

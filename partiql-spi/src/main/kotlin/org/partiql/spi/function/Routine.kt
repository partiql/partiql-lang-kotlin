package org.partiql.spi.function

import org.partiql.types.PType

/**
 * A [Routine] is a PartiQL-routine callable from an expression context.
 */
public interface Routine {

    /**
     * The function name. Required.
     */
    public fun getName(): String

    /**
     * The formal argument definitions. Optional.
     */
    public fun getParameters(): Array<Parameter> = emptyArray<Parameter>()

    /**
     * Compute the return type of the routine given the arguments.
     */
    public fun getReturnType(args: Array<PType>): PType
}

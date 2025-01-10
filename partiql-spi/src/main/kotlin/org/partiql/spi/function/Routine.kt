package org.partiql.spi.function

import org.partiql.spi.types.PType

/**
 * A [Routine] is a PartiQL-routine callable from an expression context.
 */
public interface Routine {

    /**
     * Function name. Required.
     */
    public fun getName(): String

    /**
     * Formal argument definitions. Optional with default empty.
     */
    public fun getParameters(): Array<Parameter> = emptyArray<Parameter>()

    /**
     * Compute the function return type. Required.
     */
    public fun getReturnType(args: Array<PType>): PType
}

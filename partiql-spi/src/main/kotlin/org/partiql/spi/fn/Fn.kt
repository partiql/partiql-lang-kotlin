package org.partiql.spi.fn

import org.partiql.spi.value.Datum

/**
 * Represents a scalar function (SQL row-value call expression).
 */
public interface Fn {

    /**
     * Scalar function signature.
     */
    public val signature: FnSignature

    /**
     * Invoke the function with the given arguments.
     *
     * @param args the arguments to the function
     * @return the result of the function
     */
    public fun invoke(args: Array<Datum>): Datum
}

package org.partiql.spi.function

import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Represents a scalar function (SQL row-value call expression).
 */
public interface Function : Routine {

    /**
     * Returns an invocable implementation. Optional.
     */
    public fun getInstance(args: Array<PType>): Instance {
        throw Error("Function ${getName()} has no implementations.")
    }

    /**
     * Invocable implementation of a function.
     *
     * @see Function.getInstance
     */
    public interface Instance {

        /**
         *
         * Invoke the function with the given arguments. Required.
         *
         * @param args the arguments to the function
         * @return the result of the function
         */
        public fun invoke(args: Array<Datum>): Datum
    }
}

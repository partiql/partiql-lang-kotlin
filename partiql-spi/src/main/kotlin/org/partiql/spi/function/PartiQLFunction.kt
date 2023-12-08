package org.partiql.spi.function

import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * The [PartiQLFunction] interface is used to implement user-defined-functions (UDFs).
 * UDFs can be registered to a plugin for use in the query planner and evaluator.
 */
@OptIn(PartiQLValueExperimental::class)
public sealed interface PartiQLFunction {

    /**
     * Defines the function's parameters and argument handling.
     */
    public val signature: FunctionSignature

    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : PartiQLFunction {

        /**
         * Scalar function signature.
         */
        override val signature: FunctionSignature.Scalar

        /**
         * Invoke the routine with the given arguments.
         *
         * @param args
         * @return
         */
        public fun invoke(args: Array<PartiQLValue>): PartiQLValue
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : PartiQLFunction {

        /**
         * Aggregation function signature.
         */
        override val signature: FunctionSignature.Aggregation

        /**
         * Apply args to the accumulator.
         *
         * @param args
         * @return
         */
        public fun next(args: Array<PartiQLValue>): PartiQLValue

        /**
         * Return the accumulator value.
         *
         * @return
         */
        public fun value(): PartiQLValue
    }
}

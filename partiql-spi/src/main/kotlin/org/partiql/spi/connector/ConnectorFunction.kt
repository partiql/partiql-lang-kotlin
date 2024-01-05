package org.partiql.spi.connector

import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * The [ConnectorFunction] interface is used to implement user-defined-functions (UDFs).
 * UDFs can be registered to a plugin for use in the query planner and evaluator.
 */
@ConnectorFunctionExperimental
public sealed interface ConnectorFunction {

    /**
     * Defines the function's parameters and argument handling.
     */
    public val signature: FunctionSignature

    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : ConnectorFunction {

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
        @OptIn(PartiQLValueExperimental::class)
        public fun invoke(args: Array<PartiQLValue>): PartiQLValue
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : ConnectorFunction {

        /**
         * Aggregation function signature.
         */
        override val signature: FunctionSignature.Aggregation

        /**
         * Instantiates an accumulator for this aggregation function.
         *
         * @return
         */
        public fun accumulator(): Accumulator
    }

    public interface Accumulator {

        /**
         * Apply args to the accumulator.
         *
         * @param args
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun next(args: Array<PartiQLValue>): PartiQLValue

        /**
         * Return the accumulator value.
         *
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun value(): PartiQLValue
    }
}
package org.partiql.spi.function

import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Represents a SQL aggregation function, such as MAX, MIN, SUM, etc.
 */
public interface Aggregation : Routine {

    /**
     * Instantiates a stateful accumulator for this aggregation function.
     */
    public fun getAccumulator(args: Array<PType>): Accumulator {
        throw Error("Aggregation ${getName()} has no implementations.")
    }

    /**
     * Aggregation function state.
     */
    public interface Accumulator {

        /**
         * Apply args to the accumulator.
         *
         * @param args
         * @return
         */
        public fun next(args: Array<Datum>)

        /**
         * Return the accumulator value.
         *
         * @return
         */
        public fun value(): Datum
    }

    public companion object {

        /**
         * TODO consider replacing with a builder prior to 1.0
         *
         * @param name
         * @param parameters
         * @param returns
         * @param accumulator
         * @return
         */
        @JvmStatic
        public fun static(
            name: String,
            parameters: Array<Parameter>,
            returns: PType,
            accumulator: () -> Accumulator,
        ): Aggregation = _Aggregation(name, parameters, returns, accumulator)
    }

    /**
     * Private internal aggregation implementation.
     */
    @Suppress("ClassName")
    private class _Aggregation(
        private var name: String,
        private var parameters: Array<Parameter>,
        private var returns: PType,
        private var accumulator: () -> Accumulator,
    ) : Aggregation {
        override fun getName(): String = name
        override fun getParameters(): Array<Parameter> = parameters
        override fun getReturnType(args: Array<PType>): PType = returns
        override fun getAccumulator(args: Array<PType>): Accumulator = accumulator()
        override fun toString(): String {
            val parameters = parameters.joinToString("__") { it.getType().name() }
            val returnType = returns.name()
            return "FN_${name}___${parameters}___$returnType"
        }
    }
}

package org.partiql.spi.function

import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Represents an SQL table-value expression call.
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

        @JvmStatic
        public fun standard(
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
            val parameters = parameters.joinToString("__") { it.getType().kind.name }
            val returnType = returns.kind.name
            return "FN_${name}___${parameters}___$returnType"
        }
    }
}

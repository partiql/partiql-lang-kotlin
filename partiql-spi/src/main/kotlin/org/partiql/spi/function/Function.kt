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
     * Indicates that this function returns NULL on NULL inputs; optional with default true.
     */
    public fun isNullCall(): Boolean = true

    /**
     * Invocable implementation of a function.
     *
     * @see Function.getInstance
     */
    public interface Instance {

        /**
         * Invoke the function with the given arguments. Required.
         *
         * @param args the arguments to the function
         * @return the result of the function
         */
        public fun invoke(args: Array<Datum>): Datum
    }

    /**
     * Factory methods for standard function implementations.
     */
    public companion object {

        @JvmStatic
        public fun standard(
            name: String,
            parameters: Array<Parameter>,
            returns: PType,
            invoke: (Array<Datum>) -> Datum,
        ): Function = _Function(
            name, parameters, returns,
            object : Instance {
                override fun invoke(args: Array<Datum>): Datum = invoke(args)
            }
        )
    }

    /**
     * Private internal function implementation.
     */
    @Suppress("ClassName")
    private class _Function(
        private var name: String,
        private var parameters: Array<Parameter>,
        private var returns: PType,
        private var instance: Instance,
    ) : Function {
        override fun getName(): String = name
        override fun getParameters(): Array<Parameter> = parameters
        override fun getReturnType(args: Array<PType>): PType = returns
        override fun getInstance(args: Array<PType>): Instance = instance
        override fun toString(): String {
            val parameters = parameters.joinToString("__") { it.getType().kind.name }
            val returnType = returns.kind.name
            return "FN_${name}___${parameters}___$returnType"
        }
    }
}

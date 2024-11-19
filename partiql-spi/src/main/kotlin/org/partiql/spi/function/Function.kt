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
    public fun getInstance(args: Array<PType>): Instance? {
        throw Error("Function ${getName()} has no implementations.")
    }

    /**
     * Invocable implementation of a function.
     *
     * TODO replace isNullCall and isMissingCall with routine characteristics.
     *
     * @see Function.getInstance
     */
    public abstract class Instance(
        @JvmField public val name: String,
        @JvmField public val parameters: Array<PType>,
        @JvmField public val returns: PType,
        @JvmField public val isNullCall: Boolean = true,
        @JvmField public val isMissingCall: Boolean = true,
    ) {
        /**
         * Invoke the function with the given arguments. Required.
         *
         * @param args the arguments to the function
         * @return the result of the function
         */
        public abstract fun invoke(args: Array<Datum>): Datum
    }

    /**
     * Factory methods for standard function implementations.
     */
    public companion object {

        /**
         * TODO INTERNALIZE TO SPI AND REPLACE WITH A BUILDER (OR SOMETHING..)
         *
         * @param name
         * @param parameters
         * @param returns
         * @param isNullCall
         * @param isMissingCall
         * @param invoke
         * @return
         */
        @JvmStatic
        public fun instance(
            name: String,
            parameters: Array<Parameter>,
            returns: PType,
            isNullCall: Boolean = true,
            isMissingCall: Boolean = true,
            invoke: (Array<Datum>) -> Datum,
        ): Instance {
            return object : Instance(
                name,
                Array(parameters.size) { parameters[it].getType() },
                returns,
                isNullCall,
                isMissingCall,
            ) {
                override fun invoke(args: Array<Datum>): Datum = invoke(args)
            }
        }

        /**
         * TODO INTERNALIZE TO SPI AND REPLACE WITH A BUILDER (OR SOMETHING..)
         *
         * @param name
         * @param parameters
         * @param returns
         * @param isNullCall
         * @param isMissingCall
         * @param invoke
         * @return
         */
        @JvmStatic
        public fun static(
            name: String,
            parameters: Array<Parameter>,
            returns: PType,
            isNullCall: Boolean = true,
            isMissingCall: Boolean = true,
            invoke: (Array<Datum>) -> Datum,
        ): Function = _Function(
            name, parameters, returns,
            object : Instance(
                name,
                Array(parameters.size) { parameters[it].getType() },
                returns,
                isNullCall,
                isMissingCall,
            ) {
                override fun invoke(args: Array<Datum>): Datum = invoke(args)
            },
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
            // TODO debug strings for SqlTypeFamily
            // val parameters = parameters.joinToString("__") { it.getType().kind.name }
            val returnType = returns.kind.name
            return "FN_${name}___${parameters}___$returnType"
        }
    }
}

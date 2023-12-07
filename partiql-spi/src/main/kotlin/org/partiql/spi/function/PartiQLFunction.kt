package org.partiql.spi.function

import org.partiql.types.function.FunctionSignature
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.check
import org.partiql.value.int32Value

/**
 * Represents a function interface that can be overridden by external teams.
 *
 * An implementation of this interface defines the behavior of the function
 * and its signature, which includes the function's names, return type, parameters,
 * determinism, and an optional description.
 */
public sealed interface PartiQLFunction {

    /**
     *
     */
    public val signature: FunctionSignature

    /**
     * TODO
     *
     */
    public interface Scalar : PartiQLFunction {

        /**
         *
         */
        override val signature: FunctionSignature.Scalar

        /**
         * TODO
         *
         * @param args
         * @return
         */
        public fun invoke(args: Array<PartiQLValue>): PartiQLValue
    }

    /**
     * TODO
     *
     */
    public interface Aggregation : PartiQLFunction {

        /**
         *
         */
        override val signature: FunctionSignature.Aggregation

        /**
         * TODO
         *
         * @param args
         * @return
         */
        public fun next(args: Array<PartiQLValue>): PartiQLValue

        /**
         * TODO
         *
         * @return
         */
        public fun value(): PartiQLValue
    }
}

public class MyInt32Plus : PartiQLFunction.Scalar {

    override val signature: FunctionSignature.Scalar = TODO("Not yet implemented")

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg0 = args[0].check<Int32Value>().value
        val arg1 = args[1].check<Int32Value>().value
        if (arg0 == null || arg1 == null) {
            return int32Value(null)
        }
        return int32Value(arg0 + arg1)
    }
}

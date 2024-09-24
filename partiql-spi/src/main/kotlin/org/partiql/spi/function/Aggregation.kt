package org.partiql.spi.function

import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Represents an SQL table-value expression call.
 */
public interface Aggregation : Routine {

    /**
     * TODO REMOVE ME !!
     *
     * Aggregation function signature.
     */
    public val signature: AggSignature

    override fun getName(): String = signature.name

    override fun getParameters(): Array<Parameter> = signature.parameters.toTypedArray()

    override fun getReturnType(): PType = signature.returns

    public fun isDecomposable(): Boolean = signature.isDecomposable

    /**
     * !! DO NOT OVERRIDE !!
     */
    public override fun getSpecific(): String {
        val name = getName()
        val parameters = getParameters().joinToString("__") { it.getType().kind.name }
        val returnType = getReturnType().kind.name
        return "AGG_${name}___${parameters}___$returnType"
    }

    /**
     * Instantiates a stateful accumulator for this aggregation function.
     *
     * TODO add `args: Array<PType>` parameter.
     *
     * @return
     */
    public fun accumulator(): Accumulator

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
}

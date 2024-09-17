package org.partiql.spi.fn

import org.partiql.eval.value.Datum
import org.partiql.types.PType

/**
 * Represents a scalar function (SQL row-value call expression).
 */
public interface Function : Routine {

    /**
     * Scalar function signature.
     *
     * TODO REMOVE ME
     */
    public val signature: FnSignature

    override fun getName(): String = signature.name

    override fun getParameters(): Array<Parameter> = signature.parameters.toTypedArray()

    override fun getReturnType(): PType = signature.returns

    /**
     * SQL NULL CALL -> RETURNS NULL ON NULL INPUT
     */
    public fun isNullCall(): Boolean = signature.isNullCall

    /**
     * TODO REMOVE ME
     */
    public fun isMissingCall(): Boolean = signature.isMissingCall

    /**
     * TODO REPLACE ME WITH `getInstance(args: Array<PType>)` which returns an invocable instance of this function.
     *
     * Invoke the function with the given arguments. Required.
     *
     * @param args the arguments to the function
     * @return the result of the function
     */
    public fun invoke(args: Array<Datum>): Datum

    /**
     * !! DO NOT OVERRIDE !!
     */
    public override fun getSpecific(): String {
        val name = getName().uppercase()
        val parameters = getParameters().joinToString("__") { it.getType().kind.name }
        val returnType = getReturnType().kind.name
        return "FN_${name}___${parameters}___$returnType"
    }
}

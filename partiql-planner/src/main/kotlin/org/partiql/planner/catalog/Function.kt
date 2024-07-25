package org.partiql.planner.catalog

import org.partiql.planner.internal.SqlTypes
import org.partiql.types.PType

/**
 * A [Function] is a PartiQL-routine callable from an expression context.
 */
public sealed interface Function {

    /**
     * The routine name. Required.
     */
    public fun getName(): String

    /**
     * The formal argument definitions. Optional.
     */
    public fun getParameters(): Array<Parameter> = DEFAULT_PARAMETERS

    /**
     * The function return type. Required.
     */
    public fun getReturnType(): PType.Kind

    /**
     * Compute a [PType] from the given arguments.
     */
    public fun computeReturnType(args: List<PType>): PType = SqlTypes.from(getReturnType())

    /**
     * !! DO NOT OVERRIDE !!
     */
    public fun getSpecific(): String

    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : Function {

        /**
         * SQL NULL CALL -> RETURNS NULL ON NULL INPUT
         */
        public fun isNullCall(): Boolean = true

        /**
         * !! DO NOT OVERRIDE !!
         */
        public override fun getSpecific(): String {
            val name = getName().uppercase()
            val parameters = getParameters().joinToString("__") { it.type.name }
            val returnType = getReturnType().name
            return "FN_${name}___${parameters}___$returnType"
        }
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : Function {

        public fun isDecomposable(): Boolean = true

        /**
         * !! DO NOT OVERRIDE !!
         */
        public override fun getSpecific(): String {
            val name = getName()
            val parameters = getParameters().joinToString("__") { it.type.name }
            val returnType = getReturnType().name
            return "AGG_${name}___${parameters}___$returnType"
        }
    }

    /**
     * [Parameter] is a formal argument's definition.
     *
     * @property name
     * @property type
     */
    public data class Parameter(
        @JvmField public val name: String,
        @JvmField public val type: PType.Kind,
    )

    /**
     * Memoized defaults.
     */
    public companion object {

        private val DEFAULT_PARAMETERS = emptyArray<Parameter>()

        @JvmStatic
        public fun scalar(
            name: String,
            parameters: Collection<Parameter>,
            returnType: PType.Kind,
        ): Scalar = object : Scalar {
            override fun getName(): String = name
            override fun getParameters(): Array<Parameter> = parameters.toTypedArray()
            override fun getReturnType(): PType.Kind = returnType
        }

        @JvmStatic
        public fun aggregation(
            name: String,
            parameters: Collection<Parameter>,
            returnType: PType.Kind,
        ): Aggregation = object : Aggregation {
            override fun getName(): String = name
            override fun getParameters(): Array<Parameter> = parameters.toTypedArray()
            override fun getReturnType(): PType.Kind = returnType
        }
    }
}

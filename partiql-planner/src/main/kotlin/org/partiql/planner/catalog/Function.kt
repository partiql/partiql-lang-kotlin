package org.partiql.planner.catalog

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
    public fun getReturnType(): PType

    /**
     * !! DO NOT OVERRIDE !!
     */
    public fun getSpecific(): String

    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : Function {

        /**
         * Additional function properties useful for planning. Optional.
         */
        public fun getProperties(): Properties = DEFAULT_PROPERTIES

        /**
         * !! DO NOT OVERRIDE !!
         */
        public override fun getSpecific(): String {
            val name = getName()
            val parameters = getParameters().joinToString("__") { it.type.name }
            val returnType = getReturnType().kind.name
            return "FN_${name}___${parameters}___${returnType}"
        }
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : Function {

        /**
         * !! DO NOT OVERRIDE !!
         */
        public override fun getSpecific(): String {
            val name = getName()
            val parameters = getParameters().joinToString("__") { it.type.name }
            val returnType = getReturnType().kind.name
            return "AGG_${name}___${parameters}___${returnType}"
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
     * PartiQL-function properties.
     *
     * @property isNullCall
     */
    public data class Properties(
        @JvmField public val isNullCall: Boolean = false,
    )

    /**
     * Memoized defaults.
     */
    public companion object {

        private val DEFAULT_PARAMETERS = emptyArray<Parameter>()
        private val DEFAULT_PROPERTIES = Properties(isNullCall = true)

        @JvmOverloads
        public fun scalar(
            name: String,
            parameters: Collection<Parameter>,
            returnType: PType,
            properties: Properties = DEFAULT_PROPERTIES,
        ): Scalar = object : Scalar {
            override fun getName(): String = name
            override fun getParameters(): Array<Parameter> = parameters.toTypedArray()
            override fun getReturnType(): PType = returnType
            override fun getProperties(): Properties = properties
        }
    }
}

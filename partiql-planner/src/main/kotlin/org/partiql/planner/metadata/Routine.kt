package org.partiql.planner.metadata

import org.partiql.types.PType

/**
 * A [Routine] is a PartiQL-routine callable from an expression context.
 */
public sealed interface Routine {

    /**
     * The function name. Required.
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
     * Represents an SQL row-value expression call.
     */
    public interface Operator : Routine {
        public fun getSymbol(): String
        public fun getLHS(): PType.Kind?
        public fun getRHS(): PType.Kind
    }

    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : Routine {

        /**
         * Additional function properties useful for planning. Optional.
         */
        public fun getProperties(): Properties = DEFAULT_PROPERTIES
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : Routine

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
            returnType: PType.Kind,
            properties: Properties = DEFAULT_PROPERTIES,
        ): Scalar = object : Scalar {
            override fun getName(): String = name
            override fun getParameters(): Array<Parameter> = parameters.toTypedArray()
            override fun getReturnType(): PType.Kind = returnType
            override fun getProperties(): Properties = properties
        }
    }
}

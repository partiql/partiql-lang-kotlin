package org.partiql.planner.metadata

import org.partiql.planner.intern.ptype.PType

public typealias ReturnTypeFn = (args: Array<PType>) -> PType

/**
 * A [Fn] is a PartiQL-routine callable from an expression context.
 */
public sealed interface Fn {

    /**
     * The function name. Required.
     */
    public fun getName(): String

    /**
     * The formal argument definitions. Optional.
     */
    public fun getParameters(): Collection<Parameter> = DEFAULT_PARAMETERS

    /**
     * The function return type. Required.
     */
    public fun getReturnType(): PType.Kind

    /**
     * Validate the type arguments and compute an optional return type. Optional.
     */
    public fun validate(args: Array<PType>): PType? = null
    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : Fn {

        /**
         * Additional function properties useful for planning. Optional.
         */
        public fun getProperties(): Properties = DEFAULT_PROPERTIES
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : Fn

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

        private val DEFAULT_PARAMETERS = emptyList<Parameter>()
        private val DEFAULT_PROPERTIES = Properties(isNullCall = true)

        @JvmOverloads
        public fun scalar(
            name: String,
            parameters: Collection<Parameter>,
            returnType: PType.Kind,
            properties: Properties = DEFAULT_PROPERTIES,
        ): Scalar = object : Scalar {
            override fun getName(): String = name
            override fun getParameters(): Collection<Parameter> = parameters
            override fun getReturnType(): PType.Kind = returnType
            override fun getProperties(): Properties = properties
        }

        public fun scalar(
            name: String,
            parameters: Collection<Parameter>,
            returnType: PType.Kind,
            validator: (args: Array<PType>) -> PType,
            properties: Properties = DEFAULT_PROPERTIES,
        ): Scalar = object : Scalar {
            override fun getName(): String = name
            override fun getParameters(): Collection<Parameter> = parameters
            override fun getReturnType(): PType.Kind = returnType
            override fun validate(args: Array<PType>): PType = validator(args)
            override fun getProperties(): Properties = properties
        }
    }
}

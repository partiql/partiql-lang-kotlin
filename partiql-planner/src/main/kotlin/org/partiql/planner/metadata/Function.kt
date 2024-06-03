package org.partiql.planner.metadata

/**
 * A [Function] is a PartiQL-routine callable from an expression context.
 */
public interface Function {

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
    public fun getReturnType(): TempType

    /**
     * Additional function properties useful for planning. Optional.
     */
    public fun getProperties(): Properties = DEFAULT_PROPERTIES

    /**
     * The default [Function] implementation is a POJO.
     *
     * @property name
     * @property parameters
     * @property returnType
     * @property properties
     */
    public data class Base(
        @JvmField public val name: String,
        @JvmField public val parameters: Collection<Parameter>,
        @JvmField public val returnType: TempType,
        @JvmField public val properties: Properties = DEFAULT_PROPERTIES,
    ) : Function {
        override fun getName(): String = name
        override fun getParameters(): Collection<Parameter> = parameters
        override fun getReturnType(): TempType = returnType
        override fun getProperties(): Properties = properties
    }

    /**
     * [Parameter] is a formal argument's definition.
     *
     * @property name
     * @property type
     */
    public data class Parameter(
        @JvmField public val name: String,
        @JvmField public val type: TempType,
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
    private companion object {
        val DEFAULT_PARAMETERS = emptyList<Parameter>()
        val DEFAULT_PROPERTIES = Properties(isNullCall = true)
    }
}

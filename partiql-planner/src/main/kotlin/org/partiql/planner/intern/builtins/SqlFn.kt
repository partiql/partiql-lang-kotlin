package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType

/**
 * An SqlFn is just an Fn.Scalar with a `validate()` method.
 */
internal class SqlFn(
    @JvmField val name: String,
    @JvmField val parameters: Collection<Routine.Parameter>,
    @JvmField val returnType: PType.Kind,
    @JvmField val properties: Routine.Properties = DEFAULT_PROPERTIES,
    @JvmField val validator: Validator? = null,
) : Routine.Scalar {

    override fun getName(): String = name
    override fun getProperties(): Routine.Properties = properties
    override fun getParameters(): Collection<Routine.Parameter> = parameters
    override fun getReturnType(): PType.Kind = returnType

    /**
     * Return the validator for this operator to be used during SQL validation.
     */
    fun getValidator(): Validator? = validator

    /**
     * A definition contains several variants because of overloading.
     */
    interface Definition {
        fun getVariants(): List<SqlFn>
    }

    fun interface Validator {

        /**
         * Validate the operator and compute its return type.
         */
        fun validate(args: List<PType>): PType
    }

    companion object {
        private val DEFAULT_PROPERTIES = Routine.Properties(isNullCall = true)
    }
}

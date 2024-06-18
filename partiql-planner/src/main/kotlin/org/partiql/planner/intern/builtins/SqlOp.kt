package org.partiql.planner.intern.builtins

import org.partiql.planner.metadata.Routine
import org.partiql.types.PType

/**
 * An SqlOp is just an Fn.Operator with a `getValidator()` method.
 */
internal class SqlOp(
    @JvmField val name: String,
    @JvmField val symbol: String,
    @JvmField val lhs: PType.Kind?,
    @JvmField val rhs: PType.Kind,
    @JvmField val returnType: PType.Kind,
    @JvmField val validator: Validator? = null,
) : Routine.Operator {

    override fun getName(): String = name
    override fun getSymbol(): String = symbol
    override fun getLHS(): PType.Kind? = lhs
    override fun getRHS(): PType.Kind = rhs
    override fun getReturnType(): PType.Kind = returnType

    /**
     * Return the validator for this operator to be used during SQL validation.
     */
    fun getValidator(): Validator? = validator

    /**
     * A definition contains several variants because of overloading.
     */
    interface Definition {
        fun getVariants(): List<SqlOp>
    }

    fun interface Validator {

        /**
         * Validate the operator and compute its return type.
         */
        fun validate(lhs: PType?, rhs: PType): PType
    }
}

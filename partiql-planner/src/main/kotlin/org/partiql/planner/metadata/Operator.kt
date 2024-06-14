package org.partiql.planner.metadata

import org.partiql.planner.intern.ptype.PType

/**
 * Represents an SQL row-value expression call.
 */
public interface Operator : Fn {

    public fun getSymbol(): String

    public fun getLHS(): PType.Kind?

    public fun getRHS(): PType.Kind

    public companion object {

        @JvmOverloads
        public fun create(
            name: String,
            symbol: String,
            lhs: PType.Kind? = null,
            rhs: PType.Kind,
            returnType: PType.Kind,
            validator: (args: Array<PType>) -> PType,
        ): Operator = object : Operator {
            override fun getName(): String = name
            override fun getSymbol(): String = symbol
            override fun getLHS(): PType.Kind? = lhs
            override fun getRHS(): PType.Kind = rhs
            override fun getReturnType(): PType.Kind = returnType
            override fun validate(args: Array<PType>): PType = validator(args)
        }
    }
}

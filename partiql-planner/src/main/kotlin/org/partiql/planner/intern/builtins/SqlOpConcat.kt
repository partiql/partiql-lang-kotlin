package org.partiql.planner.intern.builtins

import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * <concatenation operator> is an operator, k, that returns the character string made by joining its character
 * string operands in the order given.
 */
internal object SqlOpConcat : SqlOp.Definition {

    override fun getVariants(): List<SqlOp> = variants

    @JvmStatic
    private val variants = listOf(
        //
        concat(Kind.CHAR, Kind.CHAR, returns = Kind.CHAR) { lhs, rhs ->
            val ll = lhs!!.maxLength
            val rl = rhs.maxLength
            PType.typeChar(ll + rl)
        },
        concat(Kind.VARCHAR, Kind.VARCHAR, returns = Kind.VARCHAR) { lhs, rhs ->
            val ll = lhs!!.maxLength
            val rl = rhs.maxLength
            PType.typeChar(ll + rl)
        },
        concat(Kind.STRING, Kind.STRING, returns = Kind.STRING)
        // 4.4.2.1 Operators that operate on bit strings and return bit strings
        // concat(Kind.BIT, Kind.BIT)
        // concat(Kind.BITVAR, Kind.BITVAR)
    )

    @JvmStatic
    private fun concat(
        lhs: Kind,
        rhs: Kind,
        returns: Kind,
        validator: SqlOp.Validator? = null,
    ) = SqlOp("concat", "||", lhs, rhs, returns, validator)
}

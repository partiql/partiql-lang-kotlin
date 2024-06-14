package org.partiql.planner.intern.builtins

import org.partiql.planner.intern.SqlTypes
import org.partiql.planner.intern.ptype.PType
import org.partiql.planner.metadata.Operator

/**
 * <concatenation operator> is an operator, k, that returns the character string made by joining its character
 * string operands in the order given.
 */
internal object SqlOpConcat : SqlDefinition.Operator {

    override fun getVariants(): List<Operator> = variants

    @JvmStatic
    private val variants = SqlTypes.strings.map { concatenation(it, it) }

    @JvmStatic
    private fun concatenation(lhs: PType.Kind, rhs: PType.Kind) = Operator.create(
        name = "concat",
        symbol = "||",
        lhs = lhs,
        rhs = rhs,
        returnType = rhs,
        validator = { args ->
            val l = args[0].getMaxLength() + args[1].getMaxLength()
            PType(rhs, l)
        }
    )
}

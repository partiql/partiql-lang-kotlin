package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function
import org.partiql.types.PType.Kind
import org.partiql.types.PType.Kind.CHAR
import org.partiql.types.PType.Kind.CLOB
import org.partiql.types.PType.Kind.STRING
import org.partiql.types.PType.Kind.SYMBOL
import org.partiql.types.PType.Kind.VARCHAR

/**
 * SQL-99 CONCAT.
 *
 * <concatenation operator> is an operator || that returns the character string
 * made by joining its character string operands in the order given.
 */
internal object SqlConcat : SqlFunction {

    override fun getName(): String = "concat"

    override fun getVariants(): List<Function.Scalar> = variants

    private val variants = listOf(
        concat(CHAR, returns = CHAR),
        concat(VARCHAR, returns = VARCHAR),
        concat(STRING, returns = STRING),
        concat(SYMBOL, returns = SYMBOL),
        concat(CLOB, returns = CLOB),
    )

    private fun concat(arg: Kind, returns: Kind) = Function.scalar(
        name = getName(),
        parameters = listOf(
            Function.Parameter("lhs", arg),
            Function.Parameter("rhs", arg),
        ),
        returnType = returns
    )
}

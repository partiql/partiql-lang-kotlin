package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function
import org.partiql.types.PType
import org.partiql.types.PType.Kind.CHAR
import org.partiql.types.PType.Kind.STRING
import org.partiql.types.PType.Kind.SYMBOL
import org.partiql.types.PType.Kind.VARCHAR

/**
 * SQL-99 LOWER.
 *
 * TODO documentation.
 */
internal object SqlLower : SqlFunction {

    override fun getName(): String = "lower"

    override fun getVariants(): List<Function.Scalar> = variants

    private val variants = listOf(
        lower(CHAR, returns = CHAR),
        lower(VARCHAR, returns = VARCHAR),
        lower(STRING, returns = STRING),
        lower(SYMBOL, returns = SYMBOL),
    )

    private fun lower(arg: PType.Kind, returns: PType.Kind) = Function.scalar(
        name = getName(),
        parameters = listOf(Function.Parameter("str", arg)),
        returnType = returns
    )
}

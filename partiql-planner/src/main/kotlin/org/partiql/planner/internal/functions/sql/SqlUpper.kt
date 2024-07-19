package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function
import org.partiql.types.PType.Kind
import org.partiql.types.PType.Kind.CHAR
import org.partiql.types.PType.Kind.STRING
import org.partiql.types.PType.Kind.SYMBOL
import org.partiql.types.PType.Kind.VARCHAR

/**
 * SQL-99 UPPER.
 *
 * TODO documentation.
 */
internal object SqlUpper : SqlFunction {

    override fun getName(): String = "upper"

    override fun getVariants(): List<Function.Scalar> = variants

    private val variants = listOf(
        upper(CHAR, returns = CHAR),
        upper(VARCHAR, returns = VARCHAR),
        upper(STRING, returns = STRING),
        upper(SYMBOL, returns = SYMBOL),
    )

    private fun upper(arg: Kind, returns: Kind) = Function.scalar(
        name = getName(),
        parameters = listOf(Function.Parameter("str", arg)),
        returnType = returns
    )
}

package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function
import org.partiql.types.PType.Kind
import org.partiql.types.PType.Kind.BIGINT
import org.partiql.types.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.types.PType.Kind.DOUBLE_PRECISION
import org.partiql.types.PType.Kind.INT
import org.partiql.types.PType.Kind.INT_ARBITRARY
import org.partiql.types.PType.Kind.REAL
import org.partiql.types.PType.Kind.SMALLINT
import org.partiql.types.PType.Kind.TINYINT

internal object SqlAbs : SqlFunction {

    override fun getName() = "abs"

    override fun getVariants() = variants

    private val variants = listOf(
        abs(TINYINT, returns = TINYINT),
        abs(SMALLINT, returns = SMALLINT),
        abs(INT, returns = INT),
        abs(BIGINT, returns = BIGINT),
        abs(INT_ARBITRARY, returns = INT_ARBITRARY),
        // abs(DECIMAL, returns = DECIMAL),
        abs(DECIMAL_ARBITRARY, returns = DECIMAL_ARBITRARY),
        abs(REAL, returns = REAL),
        abs(DOUBLE_PRECISION, returns = DOUBLE_PRECISION),
    )

    private fun abs(arg: Kind, returns: Kind) = Function.scalar(
        name = getName(),
        parameters = listOf(Function.Parameter("value", arg)),
        returnType = returns,
    )
}

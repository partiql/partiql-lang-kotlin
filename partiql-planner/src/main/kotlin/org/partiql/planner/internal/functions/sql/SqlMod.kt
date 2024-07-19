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

/**
 * TODO matches 0.14, but does not match SQL-99.
 */
internal object SqlMod : SqlFunction {

    override fun getName() = "mod"

    override fun getVariants() = variants

    private val variants = listOf(
        mod(TINYINT, returns = TINYINT),
        mod(SMALLINT, returns = SMALLINT),
        mod(INT, returns = INT),
        mod(BIGINT, returns = BIGINT),
        mod(INT_ARBITRARY, returns = INT_ARBITRARY),
        // mod(DECIMAL, returns = DECIMAL),
        mod(DECIMAL_ARBITRARY, returns = DECIMAL_ARBITRARY),
        mod(REAL, returns = REAL),
        mod(DOUBLE_PRECISION, returns = DOUBLE_PRECISION),
    )

    private fun mod(arg: Kind, returns: Kind) = Function.scalar(
        name = getName(),
        parameters = listOf(
            Function.Parameter("n1", arg),
            Function.Parameter("n2", arg),
        ),
        returnType = returns,
    )
}

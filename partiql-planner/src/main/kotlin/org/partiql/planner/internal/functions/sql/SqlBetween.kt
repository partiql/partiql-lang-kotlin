package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function
import org.partiql.types.PType.Kind
import org.partiql.types.PType.Kind.BIGINT
import org.partiql.types.PType.Kind.BOOL
import org.partiql.types.PType.Kind.CHAR
import org.partiql.types.PType.Kind.DATE
import org.partiql.types.PType.Kind.DECIMAL
import org.partiql.types.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.types.PType.Kind.DOUBLE_PRECISION
import org.partiql.types.PType.Kind.INT
import org.partiql.types.PType.Kind.INT_ARBITRARY
import org.partiql.types.PType.Kind.REAL
import org.partiql.types.PType.Kind.SMALLINT
import org.partiql.types.PType.Kind.STRING
import org.partiql.types.PType.Kind.SYMBOL
import org.partiql.types.PType.Kind.TIMESTAMP_WITHOUT_TZ
import org.partiql.types.PType.Kind.TIMESTAMP_WITH_TZ
import org.partiql.types.PType.Kind.TIME_WITHOUT_TZ
import org.partiql.types.PType.Kind.TIME_WITH_TZ
import org.partiql.types.PType.Kind.TINYINT
import org.partiql.types.PType.Kind.VARCHAR

/**
 * TODO This should be modeled as its own operator rather than a function call.
 */
internal object SqlBetween : SqlFunction {

    override fun getName(): String = "between"

    override fun getVariants(): List<Function.Scalar> = variants

    private val variants = listOf(
        // numeric
        between(TINYINT, returns = BOOL),
        between(SMALLINT, returns = BOOL),
        between(INT, returns = BOOL),
        between(BIGINT, returns = BOOL),
        between(INT_ARBITRARY, returns = BOOL),
        between(DECIMAL_ARBITRARY, returns = BOOL),
        between(DECIMAL, returns = BOOL),
        between(REAL, returns = BOOL),
        between(DOUBLE_PRECISION, returns = BOOL),
        // character strings
        between(CHAR, returns = BOOL),
        between(VARCHAR, returns = BOOL),
        between(STRING, returns = BOOL),
        between(SYMBOL, returns = BOOL),
        // datetime
        between(DATE, returns = BOOL),
        between(TIME_WITH_TZ, returns = BOOL),
        between(TIME_WITHOUT_TZ, returns = BOOL),
        between(TIMESTAMP_WITH_TZ, returns = BOOL),
        between(TIMESTAMP_WITHOUT_TZ, returns = BOOL),
    )

    private fun between(arg: Kind, returns: Kind) = Function.scalar(
        name = getName(),
        parameters = listOf(
            Function.Parameter("value", arg),
            Function.Parameter("lower", arg),
            Function.Parameter("upper", arg),
        ),
        returnType = returns
    )
}

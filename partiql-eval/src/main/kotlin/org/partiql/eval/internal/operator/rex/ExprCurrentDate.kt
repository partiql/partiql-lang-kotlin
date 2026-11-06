package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.compiler.QueryDate
import org.partiql.spi.value.Datum

/**
 * `CURRENT_DATE` for one query execution.
 *
 * The date is taken from [queryDate], which is recorded when execution starts, so every
 * reference in that query returns the same [org.partiql.spi.types.PType.DATE].
 */
internal class ExprCurrentDate(private val queryDate: QueryDate) : ExprValue {

    override fun eval(env: Environment): Datum = Datum.date(queryDate.date())
}

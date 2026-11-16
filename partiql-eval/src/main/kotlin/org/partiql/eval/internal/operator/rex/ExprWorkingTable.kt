package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.operator.rel.WorkingTable
import org.partiql.spi.value.Datum

/**
 * Returns the rows of the [WorkingTable] of the enclosing recursive WITH list element.
 */
internal class ExprWorkingTable(private val workingTable: WorkingTable) : ExprValue {

    override fun eval(env: Environment): Datum = Datum.bag(workingTable.rows)
}

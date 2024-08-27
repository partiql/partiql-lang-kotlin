package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.planner.catalog.Table

/**
 * Wrap a [Table] as an expression operator.
 *
 * @constructor
 *
 * @param table
 */
internal class ExprTable(table: Table) : Operator.Expr {

    // DO NOT USE FINAL
    private var _table = table

    override fun eval(env: Environment): Datum = _table.getDatum()
}

package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum

/**
 * Wrap a [Table] as an expression operator.
 *
 * @constructor
 *
 * @param table
 */
internal class ExprTable(table: Table) : Expression {

    // DO NOT USE FINAL
    private var _table = table

    override fun eval(env: Environment): Datum = _table.getDatum()
}

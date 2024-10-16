package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum

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

    override fun eval(): Datum = _table.getDatum()
}

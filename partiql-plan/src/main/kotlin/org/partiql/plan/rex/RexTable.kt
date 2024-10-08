package org.partiql.plan.rex

import org.partiql.spi.catalog.Table

/**
 * Global variable references e.g. tables and views.
 */
public interface RexTable : Rex {

    public fun getTable(): Table

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitTable(this, ctx)
}

/**
 * Default [RexTable] implementation.
 */
internal class RexTableImpl(table: Table) : RexTable {

    // DO NOT USE FINAL
    private var _table = table
    private var _type = RexType(table.getSchema())

    override fun getTable(): Table = _table

    override fun getType(): RexType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexTable) return false
        if (_table != other.getTable()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _table.hashCode()
        return result
    }
}

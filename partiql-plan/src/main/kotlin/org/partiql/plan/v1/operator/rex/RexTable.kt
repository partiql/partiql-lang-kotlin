package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * Global variable references e.g. tables and views.
 * TODO NAMING?? RexTable??
 */
public interface RexTable : Rex {

    public fun getCatalog(): String

    /**
     * TODO replace with Catalog Name
     */
    public fun getName(): String

    override fun getOperands(): List<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitTable(this, ctx)
}

/**
 * Default [RexTable] implementation.
 */
internal class RexTableImpl(catalog: String, name: String) : RexTable {

    // DO NOT USE FINAL
    private var _catalog = catalog
    private var _name = name

    override fun getCatalog(): String = _catalog

    override fun getName(): String = _name

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexTable) return false
        if (_catalog != other.getCatalog()) return false
        if (_name != other.getName()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _catalog.hashCode()
        result = 31 * result + _name.hashCode()
        return result
    }
}

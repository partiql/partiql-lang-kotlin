package org.partiql.plan.v1.rex

/**
 * Global variable references e.g. tables and views.
 */
interface RexGlobal : Rex {

    fun getCatalog(): String

    /**
     * TODO replace with Catalog Name
     */
    fun getName(): String

    public override fun getOperands(): List<Rex> = emptyList()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitGlobal(this, ctx)

    /**
     * Default [RexGlobal] implementation.
     */
    public abstract class Base(catalog: String, name: String) : RexGlobal {

        // DO NOT USE FINAL
        private var _catalog = catalog
        private var _name = name

        public override fun getCatalog(): String = _catalog

        public override fun getName(): String = _name

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexGlobal) return false
            if (_catalog != other.getCatalog()) return false
            if (_name != other.getName()) return false
            return true
        }

        public override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _catalog.hashCode()
            result = 31 * result + _name.hashCode()
            return result
        }
    }
}

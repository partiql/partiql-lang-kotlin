package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
interface RexCollection : Rex {

    public fun getValues(): List<Rex>

    public override fun getOperands(): List<Rex> = getValues()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCollection(this, ctx)

    /**
     * Default [RexCollection] operator for extension.
     */
    public abstract class Base(values: List<Rex>) : RexCollection {

        // DO NOT USE FINAL
        private var _values = values

        public override fun getValues(): List<Rex> = _values

        public override fun getOperands(): List<Rex> = _values

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexCollection) return false
            if (_values != other.getValues()) return false
            return true
        }

        public override fun hashCode(): Int = _values.hashCode()
    }
}

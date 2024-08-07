package org.partiql.plan.operator.rex

/**
 * The subquery IN operator e.g. `<values> IN (<subquery>)`
 */
public interface RexSubqueryIn : Rex {

    public fun getInput(): org.partiql.plan.operator.rel.Rel

    public fun getValues(): List<Rex>

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubqueryIn(this, ctx)

    /**
     * The default [RexSubqueryIn] operator intended for extension.
     */
    abstract class Base(input: org.partiql.plan.operator.rel.Rel, values: List<Rex>) : RexSubqueryIn {

        // DO NOT USE FINAL
        private var _input = input
        private var _values = values

        override fun getInput(): org.partiql.plan.operator.rel.Rel = _input

        override fun getValues(): List<Rex> = _values

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexSubqueryIn) return false
            if (_input != other.getInput()) return false
            if (_values != other.getValues()) return false
            return true
        }

        override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}

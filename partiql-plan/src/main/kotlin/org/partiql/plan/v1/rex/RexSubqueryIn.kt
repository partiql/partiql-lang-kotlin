package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * The subquery IN operator e.g. `<values> IN (<subquery>)`
 */
public interface RexSubqueryIn : Rex {

    public fun getInput(): Rel

    public fun getValues(): List<Rex>

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubqueryIn(this, ctx)

    /**
     * The default [RexSubqueryIn] operator intended for extension.
     */
    public abstract class Base(input: Rel, values: List<Rex>) : RexSubqueryIn {

        // DO NOT USE FINAL
        private var _input = input
        private var _values = values

        public override fun getInput(): Rel = _input

        public override fun getValues(): List<Rex> = _values

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexSubqueryIn) return false
            if (_input != other.getInput()) return false
            if (_values != other.getValues()) return false
            return true
        }

        public override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}

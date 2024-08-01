package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * Scalar subquery coercion.
 */
public interface RexSubquery : Rex {

    public fun getInput(): Rel

    public override fun getOperands(): List<Rex> = emptyList()

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubquery(this, ctx)

    public abstract class Base(input: Rel) : RexSubquery {

        // DO NOT USE FINAL
        private var _input = input

        public override fun getInput(): Rel = _input

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexSubquery) return false
            if (_input != other.getInput()) return false
            return true
        }

        public override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}

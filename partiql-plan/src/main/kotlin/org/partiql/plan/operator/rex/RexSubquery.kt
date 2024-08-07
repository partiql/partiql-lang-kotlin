package org.partiql.plan.operator.rex

import org.partiql.types.PType

/**
 * Scalar subquery coercion.
 */
public interface RexSubquery : Rex {

    public fun getInput(): org.partiql.plan.operator.rel.Rel

    override fun getOperands(): List<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubquery(this, ctx)

    abstract class Base(input: org.partiql.plan.operator.rel.Rel) : RexSubquery {

        // DO NOT USE FINAL
        private var _input = input

        override fun getInput(): org.partiql.plan.operator.rel.Rel = _input

        override fun getType(): PType {
            TODO("Not yet implemented")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexSubquery) return false
            if (_input != other.getInput()) return false
            return true
        }

        override fun hashCode(): Int {
            return _input.hashCode()
        }
    }
}

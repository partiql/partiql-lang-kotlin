package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexLit : Rex {

    /**
     * TODO REPLACE WITH DATUM
     */
    public fun getValue(): String

    public override fun getOperands(): List<Rex> = emptyList()

    public override fun getType(): PType = TODO("Replace with datum.getType()")

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitLit(this, ctx)

    public abstract class Base(value: String) : RexLit {

        // DO NOT USE FINAL
        private var _value = value

        public override fun getValue(): String = _value

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexLit) return false
            if (_value != other.getValue()) return false
            return true
        }

        public override fun hashCode(): Int = _value.hashCode()
    }
}

package org.partiql.plan.v1.rex

import org.partiql.types.PType

/**
 * Logical `CAST` operator — ex: CAST(<operand> AS <target>).
 */
public interface RexCast : Rex {

    public fun getOperand(): Rex

    public fun getTarget(): PType

    public override fun getOperands(): List<Rex> = listOf(getOperand())

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCast(this, ctx)

    /**
     * Default [RexCast] implementation meant for extension.
     */
    public abstract class Base(operand: Rex, target: PType) : RexCast {

        // DO NOT USE FINAL
        private var _operand = operand
        private var _target = target
        private var _operands: List<Rex>? = null

        public override fun getOperand(): Rex = _operand

        public override fun getTarget(): PType = _target

        public override fun getOperands(): List<Rex> {
            if (_operands == null) {
                _operands = listOf(_operand)
            }
            return _operands!!
        }

        public override fun getType(): PType = _target
        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexCast) return false
            if (_operand != other.getOperand()) return false
            if (_target != other.getTarget()) return false
            return true
        }

        public override fun hashCode(): Int {
            var result = 1
            result = 31 * result + _operand.hashCode()
            result = 31 * result + _target.hashCode()
            return result
        }
    }
}

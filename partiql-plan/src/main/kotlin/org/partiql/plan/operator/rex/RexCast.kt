package org.partiql.plan.operator.rex

import org.partiql.types.PType

/**
 * Logical `CAST` operator — ex: CAST(<operand> AS <target>).
 */
public interface RexCast : Rex {

    public fun getOperand(): Rex

    public fun getTarget(): PType

    override fun getOperands(): List<Rex> = listOf(getOperand())

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCast(this, ctx)
}

/**
 * Default [RexCast] implementation meant for extension.
 */
internal class RexCastImpl(operand: Rex, target: PType) : RexCast {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _target = target
    private var _operands: List<Rex>? = null

    override fun getOperand(): Rex = _operand

    override fun getTarget(): PType = _target

    override fun getOperands(): List<Rex> {
        if (_operands == null) {
            _operands = listOf(_operand)
        }
        return _operands!!
    }

    override fun getType(): PType = _target
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexCast) return false
        if (_operand != other.getOperand()) return false
        if (_target != other.getTarget()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _operand.hashCode()
        result = 31 * result + _target.hashCode()
        return result
    }
}

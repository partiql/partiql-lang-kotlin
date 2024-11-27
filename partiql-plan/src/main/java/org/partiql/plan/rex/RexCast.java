package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.types.PType

/**
 * Logical `CAST` operator — ex: CAST(<operand> AS <target>).
 */
public interface RexCast : Rex {

    public fun getOperand(): Rex

    public fun getTarget(): PType

   

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitCast(this, ctx)
}

/**
 * Default [RexCast] implementation meant for extension.
 */
internal class RexCastImpl(operand: Rex, target: PType) : RexCast {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _target = target
    private var _children: List<Rex>? = null
    private var _type = RexType(_target)

    override fun getOperand(): Rex = _operand

    override fun getTarget(): PType = _target

   
        if (_children == null) {
            _children = listOf(_operand)
        }
        return _children!!
    }

    override fun getType(): RexType = _type

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

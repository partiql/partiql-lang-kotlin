package org.partiql.plan.rex

import org.partiql.plan.Visitor
import org.partiql.plan.rel.Rel
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexPivot : Rex {

    public fun getInput(): Rel

    public fun getKey(): Rex

    public fun getValue(): Rex

   

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitPivot(this, ctx)
}

/**
 * Default [RexPivot] operator.
 */
internal class RexPivotImpl(input: Rel, key: Rex, value: Rex) : RexPivot {

    // DO NOT USE FINAL
    private var _input = input
    private var _key = key
    private var _value = value

    private var children: List<Rex>? = null
    private var type: PType? = null

    override fun getInput(): Rel = _input

    override fun getKey(): Rex = _key

    override fun getValue(): Rex = _value

    override fun getType(): RexType {
        if (type == null) {
            type = PType.struct()
        }
        return RexType(type!!)
    }

   
        if (children == null) {
            children = listOf(getKey(), getValue())
        }
        return children!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexPivot) return false

        if (_input != other.getInput()) return false
        if (_key != other.getKey()) return false
        if (_value != other.getValue()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + _input.hashCode()
        result = 31 * result + _key.hashCode()
        result = 31 * result + _value.hashCode()
        return result
    }
}

package org.partiql.plan.operator.rex

import org.partiql.plan.operator.rel.Rel
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexPivot : Rex {

    public fun getInput(): Rel

    public fun getKey(): Rex

    public fun getValue(): Rex

    override fun getType(): PType = PType.typeStruct()

    override fun getOperands(): List<Rex> = listOf(getKey(), getValue())

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitPivot(this, ctx)
}

/**
 * An abstract [RexPivot] implementation intended for extension.
 */
internal class RexPivotImpl(input: Rel, key: Rex, value: Rex) : RexPivot {

    // DO NOT USE FINAL
    private var _input = input
    private var _key = key
    private var _value = value

    private var operands: List<Rex>? = null
    private var type: PType? = null

    override fun getInput(): Rel = _input

    override fun getKey(): Rex = _key

    override fun getValue(): Rex = _value

    override fun getType(): PType {
        if (type == null) {
            type = PType.typeStruct()
        }
        return type!!
    }

    override fun getOperands(): List<Rex> {
        if (operands == null) {
            operands = listOf(getKey(), getValue())
        }
        return operands!!
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

package org.partiql.plan.operator.rex

import org.partiql.plan.operator.rel.Rel
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexSelect : Rex {

    public fun getInput(): Rel

    public fun getConstructor(): Rex

    override fun getType(): PType = PType.typeBag(getConstructor().getType())

    override fun getOperands(): List<Rex> = listOf(getConstructor())

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSelect(this, ctx)
}

internal class RexSelectImpl(input: Rel, constructor: Rex) : RexSelect {

    private var _input = input
    private var _constructor = constructor

    override fun getInput(): Rel = _input

    override fun getConstructor(): Rex = _constructor

    override fun getType(): PType = PType.typeBag(_constructor.getType())

    override fun getOperands(): List<Rex> = listOf(_constructor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexSelect) return false
        if (_input != other.getInput()) return false
        if (_constructor != other.getConstructor()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = _input.hashCode()
        result = 31 * result + _constructor.hashCode()
        return result
    }
}

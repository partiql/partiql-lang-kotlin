package org.partiql.plan.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexSelect : Rex {

    public fun getInput(): org.partiql.plan.rel.Rel

    public fun getConstructor(): Rex

    override fun getType(): PType = PType.bag(getConstructor().getType())

    override fun getChildren(): Collection<Rex> = listOf(getConstructor())

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSelect(this, ctx)
}

internal class RexSelectImpl(input: org.partiql.plan.rel.Rel, constructor: Rex) : RexSelect {

    private var _input = input
    private var _constructor = constructor

    override fun getInput(): org.partiql.plan.rel.Rel = _input

    override fun getConstructor(): Rex = _constructor

    override fun getType(): PType = PType.bag(_constructor.getType())

    override fun getChildren(): Collection<Rex> = listOf(_constructor)

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

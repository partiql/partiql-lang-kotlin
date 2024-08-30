package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * TODO REMOVE ME AFTER EVALUATOR MERGE
 */
public interface RexNullIf : Rex {

    public fun getValue(): Rex

    public fun getNullifier(): Rex

    override fun getType(): PType = PType.bag(getNullifier().getType())

    override fun getChildren(): Collection<Rex> = listOf(getNullifier())

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitNullIf(this, ctx)
}

/**
 * Internal
 */
internal class RexNullIfImpl(value: Rex, nullifier: Rex) : RexNullIf {

    // DO NOT USE FINAL
    private var _value = value
    private var _nullifier = nullifier

    override fun getValue(): Rex = _value

    override fun getNullifier(): Rex = _nullifier

    override fun getType(): PType = PType.bag(_nullifier.getType())

    override fun getChildren(): Collection<Rex> = listOf(_nullifier)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexNullIf) return false
        if (_value != other.getValue()) return false
        if (_nullifier != other.getNullifier()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = _value.hashCode()
        result = 31 * result + _nullifier.hashCode()
        return result
    }
}

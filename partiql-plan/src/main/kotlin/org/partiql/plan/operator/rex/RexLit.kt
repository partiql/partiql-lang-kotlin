package org.partiql.plan.operator.rex

import org.partiql.eval.value.Datum
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexLit : Rex {

    public fun getValue(): Datum

    override fun getOperands(): List<Rex> = emptyList()

    override fun getType(): PType = getValue().type

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitLit(this, ctx)
}

internal class RexLitImpl(value: Datum) : RexLit {

    // DO NOT USE FINAL
    private var _value = value

    override fun getValue(): Datum = _value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexLit) return false
        if (_value != other.getValue()) return false
        return true
    }

    override fun hashCode(): Int = _value.hashCode()
}

package org.partiql.plan.rex

/**
 * Logical operator for the SQL NULLIF special form.
 */
public interface RexNullIf : Rex {

    public fun getV1(): Rex

    public fun getV2(): Rex

    override fun getChildren(): Collection<Rex> = listOf(getV1(), getV2())

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitNullIf(this, ctx)
}

/**
 * Internal
 */
internal class RexNullIfImpl(v1: Rex, v2: Rex) : RexNullIf {

    // DO NOT USE FINAL
    private var _v1 = v1
    private var _v2 = v2

    override fun getV1(): Rex = _v1

    override fun getV2(): Rex = _v2

    override fun getType(): RexType = _v1.getType()

    override fun getChildren(): Collection<Rex> = listOf(_v2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexNullIf) return false
        if (_v1 != other.getV1()) return false
        if (_v2 != other.getV2()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = _v1.hashCode()
        result = 31 * result + _v2.hashCode()
        return result
    }
}

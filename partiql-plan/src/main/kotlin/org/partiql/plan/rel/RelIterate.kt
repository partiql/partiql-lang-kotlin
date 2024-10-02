package org.partiql.plan.rel

import org.partiql.plan.rex.Rex

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v> AT <i>`.
 */
public interface RelIterate : Rel {

    public fun getInput(): Rex

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R =
        visitor.visitIterate(this, ctx)
}

/**
 * Default [RelIterate] implementation.
 */
internal class RelIterateImpl(input: Rex) : RelIterate {

    // DO NOT USE FINAL
    private var _input: Rex = input

    override fun getInput(): Rex = _input

    override fun getType(): RelType {
        TODO("Implement getSchema for scan")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RelIterate) return false
        return _input == other.getInput()
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }
}

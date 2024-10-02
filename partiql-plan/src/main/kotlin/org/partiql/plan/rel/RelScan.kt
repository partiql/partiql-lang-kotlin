package org.partiql.plan.rel

import org.partiql.plan.rex.Rex

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v>`.
 */
public interface RelScan : Rel {

    public fun getInput(): Rex

    override fun getChildren(): Collection<Rel> = emptyList()

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitScan(this, ctx)
}

/**
 * Default [RelScan] implementation.
 */
internal class RelScanImpl(input: Rex) : RelScan {

    // DO NOT USE FINAL
    private var _input: Rex = input

    override fun getInput(): Rex = _input

    override fun getType(): RelType {
        TODO("Implement getSchema for scan")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RelScan) return false
        return _input == other.getInput()
    }

    override fun hashCode(): Int {
        return _input.hashCode()
    }
}

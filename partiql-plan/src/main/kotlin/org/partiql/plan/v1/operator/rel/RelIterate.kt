package org.partiql.plan.v1.operator.rel

import org.partiql.plan.v1.Schema
import org.partiql.plan.v1.operator.rex.Rex
import org.partiql.types.PType

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v> AT <i>`.
 */
public interface RelIterate : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = getInput().getType().kind == PType.Kind.ARRAY

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitIterate(this, ctx)
}

/**
 * Default [RelIterate] implementation.
 */
internal class RelIterateImpl(input: Rex) : RelIterate {

    // DO NOT USE FINAL
    private var _input: Rex = input

    override fun getInput(): Rex = _input

    override fun getSchema(): Schema {
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

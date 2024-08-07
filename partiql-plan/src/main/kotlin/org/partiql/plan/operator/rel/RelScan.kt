package org.partiql.plan.operator.rel

import org.partiql.plan.operator.rex.Rex
import org.partiql.types.PType

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v>`.
 */
public interface RelScan : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = getInput().getType().kind == PType.Kind.LIST

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitScan(this, ctx)
}

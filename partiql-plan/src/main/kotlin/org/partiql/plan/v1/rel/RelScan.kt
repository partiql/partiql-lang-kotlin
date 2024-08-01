package org.partiql.plan.v1.rel

import org.partiql.plan.v1.rex.Rex
import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RelScan : Rel {

    public fun getInput(): Rex

    override fun getInputs(): List<Rel> = emptyList()

    override fun isOrdered(): Boolean = getInput().getType().kind == PType.Kind.LIST

    public override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitScan(this, ctx)
}

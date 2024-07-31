package org.partiql.plan.v1.rel

import org.partiql.plan.v1.Node
import org.partiql.plan.v1.Schema

public interface Rel : Node {

    public fun getInputs(): List<Rel>

    public fun getSchema(): Schema

    public fun isOrdered(): Boolean

    public fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R
}

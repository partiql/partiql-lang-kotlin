package org.partiql.plan.logical

/**
 * Interface for PartiQL Logical Plans.
 */
public interface Plan : Node {

    public fun getVersion(): Version

    // public fun getCatalogs(): List<Catalog> = emptyList()

    public fun getStatement(): Statement

    public override fun getChildren(): List<Node> {
        val kids = mutableListOf<Node>()
        // kids.addAll(catalogs)
        kids.add(getStatement())
        return kids
    }

    // public override fun <R, C> accept(visitor: PlanVisitor<R, C>, ctx: C): R =
    //     visitor.visitPartiQLPlan(this, ctx)

    public companion object {
        @JvmStatic
        public fun builder(): Unit = TODO("LogicalPlan builder not implemented")
    }
}

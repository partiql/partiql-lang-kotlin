package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexStruct : Rex {

    public fun getFields(): List<Field>

    /**
     * TODO DOCUMENTATION
     */
    public interface Field {

        public fun getKey(): Rex

        public fun getValue(): Rex
    }

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexStruct(this, ctx)
}

package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexPath : Rex {

    public fun getRoot(): Rex

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexPath(this, ctx)

    /**
     * TODO DOCUMENTATION
     */
    public interface Index : RexPath {

        public fun getIndex(): Rex

        public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexPathIndex(this, ctx)
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Key : RexPath {

        public fun getKey(): Rex

        public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexPathKey(this, ctx)
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Symbol : RexPath {

        public fun getSymbol(): String

        public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexPathSymbol(this, ctx)
    }
}

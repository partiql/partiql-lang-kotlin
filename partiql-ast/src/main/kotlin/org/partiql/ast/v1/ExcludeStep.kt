package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class ExcludeStep : AstNode() {
    /**
     * TODO docs, equals, hashcode
     */
    public class StructField(
        @JvmField
        public var symbol: Identifier.Symbol,
    ) : ExcludeStep() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(symbol)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExcludeStepStructField(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class CollIndex(
        @JvmField
        public var index: Int,
    ) : ExcludeStep() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExcludeStepCollIndex(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object StructWildcard : ExcludeStep() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExcludeStepStructWildcard(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object CollWildcard : ExcludeStep() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExcludeStepCollWildcard(this, ctx)
    }
}

package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class Exclude(
    @JvmField
    public var items: List<Item>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(items)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExclude(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public class Item(
        @JvmField
        public var root: Expr.Var,
        @JvmField
        public var steps: List<Step>,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(root)
            kids.addAll(steps)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExcludeItem(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public abstract class Step : AstNode() {
        /**
         * TODO docs, equals, hashcode
         */
        public class StructField(
            @JvmField
            public var symbol: Identifier.Symbol,
        ) : Step() {
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
        ) : Step() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExcludeStepCollIndex(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public object StructWildcard : Step() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExcludeStepStructWildcard(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public object CollWildcard : Step() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitExcludeStepCollWildcard(this, ctx)
        }
    }
}

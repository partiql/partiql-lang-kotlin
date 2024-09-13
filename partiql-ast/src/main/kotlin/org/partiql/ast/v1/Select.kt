package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Select : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Star -> visitor.visitSelectStar(this, ctx)
        is Project -> visitor.visitSelectProject(this, ctx)
        is Pivot -> visitor.visitSelectPivot(this, ctx)
        is Value -> visitor.visitSelectValue(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Star(
        @JvmField
        public var setq: SetQuantifier?,
    ) : Select() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectStar(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Project(
        @JvmField
        public var items: List<Item>,
        @JvmField
        public var setq: SetQuantifier?,
    ) : Select() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.addAll(items)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectProject(this, ctx)

        /**
         * TODO docs, equals, hashcode
         */
        public abstract class Item : AstNode() {
            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
                is All -> visitor.visitSelectProjectItemAll(this, ctx)
                is Expression -> visitor.visitSelectProjectItemExpression(this, ctx)
                else -> throw NotImplementedError()
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class All(
                @JvmField
                public var expr: Expr,
            ) : Item() {
                public override fun children(): Collection<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(expr)
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitSelectProjectItemAll(this, ctx)
            }

            /**
             * TODO docs, equals, hashcode
             */
            public class Expression(
                @JvmField
                public var expr: Expr,
                @JvmField
                public var asAlias: Identifier.Symbol?,
            ) : Item() {
                public override fun children(): Collection<AstNode> {
                    val kids = mutableListOf<AstNode?>()
                    kids.add(expr)
                    asAlias?.let { kids.add(it) }
                    return kids.filterNotNull()
                }

                public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                    visitor.visitSelectProjectItemExpression(this, ctx)
            }
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Pivot(
        @JvmField
        public var key: Expr,
        @JvmField
        public var `value`: Expr,
    ) : Select() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(key)
            kids.add(value)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectPivot(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Value(
        @JvmField
        public var `constructor`: Expr,
        @JvmField
        public var setq: SetQuantifier?,
    ) : Select() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(constructor)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitSelectValue(this, ctx)
    }
}

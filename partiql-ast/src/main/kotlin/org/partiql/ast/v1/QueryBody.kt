package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class QueryBody : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is SFW -> visitor.visitQueryBodySFW(this, ctx)
        is SetOp -> visitor.visitQueryBodySetOp(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class SFW(
        @JvmField
        public var select: Select,
        @JvmField
        public var exclude: Exclude?,
        @JvmField
        public var from: From,
        @JvmField
        public var let: Let?,
        @JvmField
        public var `where`: Expr?,
        @JvmField
        public var groupBy: GroupBy?,
        @JvmField
        public var having: Expr?,
    ) : QueryBody() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(select)
            exclude?.let { kids.add(it) }
            kids.add(from)
            let?.let { kids.add(it) }
            where?.let { kids.add(it) }
            groupBy?.let { kids.add(it) }
            having?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitQueryBodySFW(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class SetOp(
        @JvmField
        public var type: org.partiql.ast.v1.SetOp,
        @JvmField
        public var isOuter: Boolean,
        @JvmField
        public var lhs: Expr,
        @JvmField
        public var rhs: Expr,
    ) : QueryBody() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(type)
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitQueryBodySetOp(this, ctx)
    }
}

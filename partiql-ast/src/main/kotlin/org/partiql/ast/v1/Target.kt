package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Target : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Domain -> visitor.visitTargetDomain(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Domain(
        @JvmField
        public var statement: Statement,
        @JvmField
        public var type: String?,
        @JvmField
        public var format: String?,
    ) : Target() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(statement)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitTargetDomain(this, ctx)
    }
}

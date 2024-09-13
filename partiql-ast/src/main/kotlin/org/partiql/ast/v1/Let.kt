package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class Let(
    @JvmField
    public var bindings: List<Binding>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(bindings)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitLet(
        this,
        ctx
    )

    /**
     * TODO docs, equals, hashcode
     */
    public class Binding(
        @JvmField
        public var expr: Expr,
        @JvmField
        public var asAlias: Identifier.Symbol,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            kids.add(asAlias)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitLetBinding(this, ctx)
    }
}

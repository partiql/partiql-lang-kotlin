package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class PathLitStep : AstNode() {
    /**
     * TODO docs, equals, hashcode
     */
    public class Symbol(
        @JvmField
        public var symbol: Identifier.Symbol,
    ) : PathLitStep() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(symbol)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitPathLitStepSymbol(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Index(
        @JvmField
        public var index: Int,
    ) : PathLitStep() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitPathLitStepIndex(this, ctx)
    }
}

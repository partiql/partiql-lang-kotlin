package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Identifier : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Symbol -> visitor.visitIdentifierSymbol(this, ctx)
        is Qualified -> visitor.visitIdentifierQualified(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Symbol(
        @JvmField
        public var symbol: String,
        @JvmField
        public var caseSensitivity: CaseSensitivity,
    ) : Identifier() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitIdentifierSymbol(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Qualified(
        @JvmField
        public var root: Symbol,
        @JvmField
        public var steps: List<Symbol>,
    ) : Identifier() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(root)
            kids.addAll(steps)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitIdentifierQualified(this, ctx)
    }
}

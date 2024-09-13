package org.partiql.ast.v1

import kotlin.Int

/**
 * TODO docs, equals, hashcode
 */
public class PathLit(
    @JvmField
    public var root: Identifier.Symbol,
    @JvmField
    public var steps: List<Step>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(root)
        kids.addAll(steps)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitPath(
        this,
        ctx
    )

    /**
     * TODO docs, equals, hashcode
     */
    public abstract class Step : AstNode() {
        /**
         * TODO docs, equals, hashcode
         */
        public class Symbol(
            @JvmField
            public var symbol: Identifier.Symbol,
        ) : Step() {
            public override fun children(): Collection<AstNode> {
                val kids = mutableListOf<AstNode?>()
                kids.add(symbol)
                return kids.filterNotNull()
            }

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitPathStepSymbol(this, ctx)
        }

        /**
         * TODO docs, equals, hashcode
         */
        public class Index(
            @JvmField
            public var index: Int,
        ) : Step() {
            public override fun children(): Collection<AstNode> = emptyList()

            public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
                visitor.visitPathStepIndex(this, ctx)
        }
    }
}

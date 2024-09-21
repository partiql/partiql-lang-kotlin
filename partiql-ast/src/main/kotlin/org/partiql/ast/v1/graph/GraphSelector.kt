package org.partiql.ast.v1.graph

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphSelector : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is AnyShortest -> visitor.visitGraphSelectorAnyShortest(this, ctx)
        is AllShortest -> visitor.visitGraphSelectorAllShortest(this, ctx)
        is Any -> visitor.visitGraphSelectorAny(this, ctx)
        is AnyK -> visitor.visitGraphSelectorAnyK(this, ctx)
        is ShortestK -> visitor.visitGraphSelectorShortestK(this, ctx)
        is ShortestKGroup -> visitor.visitGraphSelectorShortestKGroup(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object AnyShortest : GraphSelector() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphSelectorAnyShortest(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object AllShortest : GraphSelector() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphSelectorAllShortest(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Any : GraphSelector() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphSelectorAny(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class AnyK(
        @JvmField
        public var k: Long,
    ) : GraphSelector() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphSelectorAnyK(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class ShortestK(
        @JvmField
        public var k: Long,
    ) : GraphSelector() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphSelectorShortestK(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class ShortestKGroup(
        @JvmField
        public var k: Long,
    ) : GraphSelector() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphSelectorShortestKGroup(this, ctx)
    }
}

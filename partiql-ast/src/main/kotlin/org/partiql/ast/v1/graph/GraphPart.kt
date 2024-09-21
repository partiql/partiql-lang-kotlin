package org.partiql.ast.v1.graph

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphPart : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Node -> visitor.visitGraphPartNode(this, ctx)
        is Edge -> visitor.visitGraphPartEdge(this, ctx)
        is Pattern -> visitor.visitGraphPartPattern(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Node(
        @JvmField
        public var prefilter: Expr?,
        @JvmField
        public var variable: String?,
        @JvmField
        public var label: GraphLabel?,
    ) : GraphPart() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            prefilter?.let { kids.add(it) }
            label?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphPartNode(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Edge(
        @JvmField
        public var direction: GraphDirection,
        @JvmField
        public var quantifier: GraphQuantifier?,
        @JvmField
        public var prefilter: Expr?,
        @JvmField
        public var variable: String?,
        @JvmField
        public var label: GraphLabel?,
    ) : GraphPart() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            quantifier?.let { kids.add(it) }
            prefilter?.let { kids.add(it) }
            label?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphPartEdge(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Pattern(
        @JvmField
        public var pattern: GraphPattern,
    ) : GraphPart() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(pattern)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphPartPattern(this, ctx)
    }
}

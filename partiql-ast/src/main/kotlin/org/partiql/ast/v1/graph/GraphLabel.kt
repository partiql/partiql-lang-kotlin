package org.partiql.ast.v1.graph

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphLabel : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Name -> visitor.visitGraphLabelName(this, ctx)
        is Wildcard -> visitor.visitGraphLabelWildcard(this, ctx)
        is Negation -> visitor.visitGraphLabelNegation(this, ctx)
        is Conj -> visitor.visitGraphLabelConj(this, ctx)
        is Disj -> visitor.visitGraphLabelDisj(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Name(
        @JvmField
        public var name: String,
    ) : GraphLabel() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphLabelName(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Wildcard : GraphLabel() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphLabelWildcard(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Negation(
        @JvmField
        public var arg: GraphLabel,
    ) : GraphLabel() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(arg)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphLabelNegation(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Conj(
        @JvmField
        public var lhs: GraphLabel,
        @JvmField
        public var rhs: GraphLabel,
    ) : GraphLabel() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphLabelConj(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Disj(
        @JvmField
        public var lhs: GraphLabel,
        @JvmField
        public var rhs: GraphLabel,
    ) : GraphLabel() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(lhs)
            kids.add(rhs)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGraphLabelDisj(this, ctx)
    }
}

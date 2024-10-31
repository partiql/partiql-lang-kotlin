package org.partiql.ast.v1.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphPart extends AstNode {
    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Node extends GraphPart {
        @Nullable
        public final Expr prefilter;

        @Nullable
        public final String variable;

        @Nullable
        public final GraphLabel label;

        public Node(@Nullable Expr prefilter, @Nullable String variable, @Nullable GraphLabel label) {
            this.prefilter = prefilter;
            this.variable = variable;
            this.label = label;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (prefilter != null) {
                kids.add(prefilter);
            }
            if (label != null) {
                kids.add(label);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphPartNode(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Edge extends GraphPart {
        @NotNull
        public final GraphDirection direction;

        @Nullable
        public final GraphQuantifier quantifier;

        @Nullable
        public final Expr prefilter;

        @Nullable
        public final String variable;

        @Nullable
        public final GraphLabel label;

        public Edge(@NotNull GraphDirection direction, @Nullable GraphQuantifier quantifier,
        @Nullable Expr prefilter, @Nullable String variable, @Nullable GraphLabel label) {
        this.direction = direction;
        this.quantifier = quantifier;
        this.prefilter = prefilter;
        this.variable = variable;
        this.label = label;
    }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (quantifier != null) {
                kids.add(quantifier);
            }
            if (prefilter != null) {
                kids.add(prefilter);
            }
            if (label != null) {
                kids.add(label);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphPartEdge(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Pattern extends GraphPart {
        @NotNull
        public final GraphPattern pattern;

        public Pattern(@NotNull GraphPattern pattern) {
            this.pattern = pattern;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(pattern);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphPartPattern(this, ctx);
        }
    }
}

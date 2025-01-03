package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class for graph pattern parts.
 */
public abstract class GraphPart extends AstNode {
    /**
     * A single node in a graph pattern.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Node extends GraphPart {
        @Nullable
        @Getter
        private final Expr prefilter;

        @Nullable
        @Getter
        private final String variable;

        @Nullable
        @Getter
        private final GraphLabel label;

        public Node(@Nullable Expr prefilter, @Nullable String variable, @Nullable GraphLabel label) {
            this.prefilter = prefilter;
            this.variable = variable;
            this.label = label;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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
     * A single edge in a graph pattern.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Edge extends GraphPart {
        @NotNull
        @Getter
        private final GraphDirection direction;

        @Nullable
        @Getter
        private final GraphQuantifier quantifier;

        @Nullable
        @Getter
        private final Expr prefilter;

        @Nullable
        @Getter
        private final String variable;

        @Nullable
        @Getter
        private final GraphLabel label;

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
        public List<AstNode> getChildren() {
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
        @Getter
        private final GraphPattern pattern;

        public Pattern(@NotNull GraphPattern pattern) {
            this.pattern = pattern;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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

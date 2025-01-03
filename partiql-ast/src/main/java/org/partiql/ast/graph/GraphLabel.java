package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A label spec in a node pattern like {@code MATCH (x : <lab>)} or in an edge pattern like {@code MATCH −[t : <lab>]−>}.
 */
public abstract class GraphLabel extends AstNode {
    /**
     * Name label as in {@code MATCH (x:Account)} or {@code MATCH -[x:Transfer]->}.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Name extends GraphLabel {
        @NotNull
        @Getter
        private final String name;

        public Name(@NotNull String name) {
            this.name = name;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelName(this, ctx);
        }
    }

    /**
     * Wildcard label as in {@code MATCH (x: %)}.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Wildcard extends GraphLabel {
        public Wildcard() {}

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelWildcard(this, ctx);
        }
    }

    /**
     * Negation label as in {@code MATCH (x: !Account)}.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Negation extends GraphLabel {
        @NotNull
        @Getter
        private final GraphLabel arg;

        public Negation(@NotNull GraphLabel arg) {
            this.arg = arg;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(arg);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelNegation(this, ctx);
        }
    }

    /**
     * Conjunction label as in {@code MATCH (x: City&Country)} - like Monaco.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Conj extends GraphLabel {
        @NotNull
        @Getter
        private final GraphLabel lhs;

        @NotNull
        @Getter
        private final GraphLabel rhs;

        public Conj(@NotNull GraphLabel lhs, @NotNull GraphLabel rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(lhs);
            kids.add(rhs);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelConj(this, ctx);
        }
    }

    /**
     * Disjunction label as in {@code MATCH (x: City|Country)} - like Paris or Germany.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Disj extends GraphLabel {
        @NotNull
        @Getter
        private final GraphLabel lhs;

        @NotNull
        @Getter
        private final GraphLabel rhs;

        public Disj(@NotNull GraphLabel lhs, @NotNull GraphLabel rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(lhs);
            kids.add(rhs);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelDisj(this, ctx);
        }
    }
}

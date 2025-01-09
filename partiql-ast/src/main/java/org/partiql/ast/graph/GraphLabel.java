package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A label spec in a node pattern like {@code MATCH (x : <lab>)} or in an edge pattern like {@code MATCH −[t : <lab>]−>}.
 * <p>
 * Note: this is an experimental API and subject to change without prior notice.
 */
public abstract class GraphLabel extends AstNode {
    /**
     * Name label as in {@code MATCH (x:Account)} or {@code MATCH -[x:Transfer]->}.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Name extends GraphLabel {
        @NotNull
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

        @NotNull
        public String getName() {
            return this.name;
        }
    }

    /**
     * Wildcard label as in {@code MATCH (x: %)}.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
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
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Negation extends GraphLabel {
        @NotNull
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

        @NotNull
        public GraphLabel getArg() {
            return this.arg;
        }
    }

    /**
     * Conjunction label as in {@code MATCH (x: City&Country)} - like Monaco.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Conj extends GraphLabel {
        @NotNull
        private final GraphLabel lhs;

        @NotNull
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

        @NotNull
        public GraphLabel getLhs() {
            return this.lhs;
        }

        @NotNull
        public GraphLabel getRhs() {
            return this.rhs;
        }
    }

    /**
     * Disjunction label as in {@code MATCH (x: City|Country)} - like Paris or Germany.
     * <p>
     * Note: this is an experimental API and subject to change without prior notice.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Disj extends GraphLabel {
        @NotNull
        private final GraphLabel lhs;

        @NotNull
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

        @NotNull
        public GraphLabel getLhs() {
            return this.lhs;
        }

        @NotNull
        public GraphLabel getRhs() {
            return this.rhs;
        }
    }
}

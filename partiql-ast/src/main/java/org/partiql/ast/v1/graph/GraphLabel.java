package org.partiql.ast.v1.graph;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class GraphLabel extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof Name) {
            return visitor.visitGraphLabelName((Name) this, ctx);
        } else if (this instanceof Wildcard) {
            return visitor.visitGraphLabelWildcard((Wildcard) this, ctx);
        } else if (this instanceof Negation) {
            return visitor.visitGraphLabelNegation((Negation) this, ctx);
        } else if (this instanceof Conj) {
            return visitor.visitGraphLabelConj((Conj) this, ctx);
        } else if (this instanceof Disj) {
            return visitor.visitGraphLabelDisj((Disj) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Name extends GraphLabel {
        @NotNull
        public String name;

        public Name(@NotNull String name) {
            this.name = name;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelName(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Wildcard extends GraphLabel {
        @Override
        @NotNull
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelWildcard(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Negation extends GraphLabel {
        @NotNull
        public GraphLabel arg;

        public Negation(@NotNull GraphLabel arg) {
            this.arg = arg;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
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
     * TODO docs, equals, hashcode
     */
    public static class Conj extends GraphLabel {
        @NotNull
        public List<GraphLabel> args;

        public Conj(@NotNull List<GraphLabel> args) {
            this.args = args;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            return new ArrayList<>(args);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelConj(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Disj extends GraphLabel {
        @NotNull
        public List<GraphLabel> args;

        public Disj(@NotNull List<GraphLabel> args) {
            this.args = args;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            return new ArrayList<>(args);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitGraphLabelDisj(this, ctx);
        }
    }
}

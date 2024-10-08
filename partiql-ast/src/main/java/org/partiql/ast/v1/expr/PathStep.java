package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class PathStep extends AstNode {
    @Nullable
    public PathStep next;

    /**
     * TODO docs, equals, hashcode
     */
    public static class Field extends PathStep {
        @NotNull
        public Identifier field;

        public Field(@NotNull Identifier field, @Nullable PathStep next) {
            this.field = field;
            this.next = next;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (next != null) {
                kids.add(next);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepField(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Element extends PathStep {
        @NotNull
        public Expr element;

        public Element(@NotNull Expr element, @Nullable PathStep next) {
            this.element = element;
            this.next = next;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(element);
            if (next != null) {
                kids.add(next);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepElement(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class AllElements extends PathStep {
        public AllElements(@Nullable PathStep next) {
            this.next = next;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (next != null) {
                kids.add(next);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepAllElements(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class AllFields extends PathStep {
        public AllFields(@Nullable PathStep next) {
            this.next = next;
        }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            if (next != null) {
                kids.add(next);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepAllFields(this, ctx);
        }
    }
}

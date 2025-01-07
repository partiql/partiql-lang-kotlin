package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A path step in a path expression.
 */
public abstract class PathStep extends AstNode {
    @Nullable
    private final PathStep next;

    protected PathStep(@Nullable PathStep _next) {
        this.next = _next;
    }

    @Nullable
    public PathStep getNext() {
        return this.next;
    }

    /**
     * A path step that represents a field referenced with the dot notation. E.g. {@code a.b}.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Field extends PathStep {
        @NotNull
        private final Identifier field;

        public Field(@NotNull Identifier field, @Nullable PathStep next) {
            super(next);
            this.field = field;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            PathStep next = getNext();
            if (next != null) {
                kids.add(next);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepField(this, ctx);
        }

        @NotNull
        public Identifier getField() {
            return this.field;
        }
    }

    /**
     * A path step that represents an element reference with the bracket notation. E.g. {@code a[0]} or {@code a['b']}
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Element extends PathStep {
        @NotNull
        private final Expr element;

        public Element(@NotNull Expr element, @Nullable PathStep next) {
            super(next);
            this.element = element;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(element);
            PathStep next = getNext();
            if (next != null) {
                kids.add(next);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepElement(this, ctx);
        }

        @NotNull
        public Expr getElement() {
            return this.element;
        }
    }

    /**
     * A path step with a wildcard within square brackets. E.g. {@code a[*]}.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class AllElements extends PathStep {
        public AllElements(@Nullable PathStep next) {
            super(next);
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            PathStep next = getNext();
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
     * A path step with a wildcard within dot notation. E.g. {@code a.*}.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class AllFields extends PathStep {
        public AllFields(@Nullable PathStep next) {
            super(next);
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            PathStep next = getNext();
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

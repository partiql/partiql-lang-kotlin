package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A path step in a path expression.
 */
public abstract class PathStep extends AstNode {
    /**
     * A path step that represents a field referenced with the dot notation. E.g. {@code a.b}.
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Field extends PathStep {
        @NotNull
        private final Identifier field;

        public Field(@NotNull Identifier field) {
            this.field = field;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(field);
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

        public Element(@NotNull Expr element) {
            this.element = element;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(element);
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
        public AllElements() {}

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return new ArrayList<>();
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
        public AllFields() {}

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathStepAllFields(this, ctx);
        }
    }
}

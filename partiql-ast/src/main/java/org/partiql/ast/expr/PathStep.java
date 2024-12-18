package org.partiql.ast.expr;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class PathStep extends AstNode {
    @Nullable
    @Getter
    protected final PathStep next;

    protected PathStep(@Nullable PathStep _next) {
        this.next = _next;
    }

    /**
     * TODO docs, equals, hashcode
     */
    @EqualsAndHashCode(callSuper = false)
    public static class Field extends PathStep {
        @NotNull
        @Getter
        private final Identifier field;

        public Field(@NotNull Identifier field, @Nullable PathStep next) {
            super(next);
            this.field = field;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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
    @EqualsAndHashCode(callSuper = false)
    public static class Element extends PathStep {
        @NotNull
        @Getter
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
    @EqualsAndHashCode(callSuper = false)
    public static class AllElements extends PathStep {
        public AllElements(@Nullable PathStep next) {
            super(next);
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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
    @EqualsAndHashCode(callSuper = false)
    public static class AllFields extends PathStep {
        public AllFields(@Nullable PathStep next) {
            super(next);
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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

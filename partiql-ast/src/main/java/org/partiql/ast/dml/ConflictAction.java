package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the mandatory action of the ON CONFLICT clause.
 *
 * @see Insert#getOnConflict()
 * @see OnConflict#getAction()
 */
public abstract class ConflictAction extends AstNode {

    /**
     * This is the DO NOTHING variant of the conflict action.
     *
     * @see ConflictAction
     * @see OnConflict#getAction()
     * @see Insert#getOnConflict()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class DoNothing extends ConflictAction {
        public DoNothing() {
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictActionDoNothing(this, ctx);
        }
    }

    /**
     * This is the DO REPLACE variant of the conflict action.
     *
     * @see ConflictAction
     * @see OnConflict#getAction()
     * @see Insert#getOnConflict()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class DoReplace extends ConflictAction {
        @NotNull
        private final DoReplaceAction action;

        @Nullable
        private final Expr condition;

        public DoReplace(@NotNull DoReplaceAction action, @Nullable Expr condition) {
            this.action = action;
            this.condition = condition;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> children = new ArrayList<>();
            children.add(action);
            if (condition != null) {
                children.add(condition);
            }
            return children;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictActionDoReplace(this, ctx);
        }

        @NotNull
        public DoReplaceAction getAction() {
            return this.action;
        }

        @Nullable
        public Expr getCondition() {
            return this.condition;
        }
    }

    /**
     * This is the DO UPDATE variant of the conflict action.
     *
     * @see ConflictAction
     * @see OnConflict#getAction()
     * @see Insert#getOnConflict()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class DoUpdate extends ConflictAction {
        @NotNull
        private final DoUpdateAction action;

        @Nullable
        private final Expr condition;

        public DoUpdate(@NotNull DoUpdateAction action, @Nullable Expr condition) {
            this.action = action;
            this.condition = condition;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> children = new ArrayList<>();
            children.add(action);
            if (condition != null) {
                children.add(condition);
            }
            return children;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictActionDoUpdate(this, ctx);
        }

        @NotNull
        public DoUpdateAction getAction() {
            return this.action;
        }

        @Nullable
        public Expr getCondition() {
            return this.condition;
        }
    }
}

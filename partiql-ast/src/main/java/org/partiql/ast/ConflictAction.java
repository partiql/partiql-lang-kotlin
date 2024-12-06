package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the mandatory action of the ON CONFLICT clause.
 * @see Insert#onConflict
 * @see OnConflict#action
 */
public abstract class ConflictAction extends AstNode {

    /**
     * This is the DO NOTHING variant of the conflict action.
     * @see ConflictAction
     * @see OnConflict#action
     * @see Insert#onConflict
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class DoNothing extends ConflictAction {
        // TODO: Equals and hashcode

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictActionDoNothing(this, ctx);
        }
    }

    /**
     * This is the DO REPLACE variant of the conflict action.
     * @see ConflictAction
     * @see OnConflict#action
     * @see Insert#onConflict
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class DoReplace extends ConflictAction {
        // TODO: Equals and hashcode

        /**
         * TODO
         */
        @NotNull
        public final DoReplaceAction action;

        /**
         * TODO
         */
        @Nullable
        public final Expr condition;

        /**
         * TODO
         * @param action TODO
         * @param condition TODO
         */
        DoReplace(@NotNull DoReplaceAction action, @Nullable Expr condition) {
            this.action = action;
            this.condition = condition;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
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
    }

    /**
     * This is the DO UPDATE variant of the conflict action.
     * @see ConflictAction
     * @see OnConflict#action
     * @see Insert#onConflict
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class DoUpdate extends ConflictAction {
        // TODO: Equals and hashcode

        /**
         * TODO
         */
        @NotNull
        public final DoUpdateAction action;

        /**
         * TODO
         */
        @Nullable
        public final Expr condition;

        /**
         * TODO
         * @param action TODO
         * @param condition TODO
         */
        DoUpdate(@NotNull DoUpdateAction action, @Nullable Expr condition) {
            this.action = action;
            this.condition = condition;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
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
    }
}

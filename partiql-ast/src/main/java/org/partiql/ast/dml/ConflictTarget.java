package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This represents the potential targets for the ON CONFLICT clause.
 * @see OnConflict
 * @see OnConflict#target
 */
public abstract class ConflictTarget extends AstNode {

    /**
     * This is the index variant of the conflict target.
     * @see OnConflict
     * @see ConflictTarget
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Index extends ConflictTarget {
        /**
         * TODO
         */
        // TODO: Should this be a list of identifiers? Or paths? Expressions?
        @NotNull
        public final List<Identifier> indexes;

        /**
         * TODO
         * @param indexes TODO
         */
        public Index(@NotNull List<Identifier> indexes) {
            this.indexes = indexes;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>(indexes);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictTargetIndex(this, ctx);
        }
    }

    /**
     * This is the ON CONSTRAINT variant of the conflict target.
     * @see OnConflict
     * @see ConflictTarget
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Constraint extends ConflictTarget {
        /**
         * TODO
         */
        // TODO: Should this be a qualified identifier?
        @NotNull
        public final Identifier constraintName;

        /**
         * TODO
         * @param constraintName TODO
         */
        public Constraint(@NotNull Identifier constraintName) {
            this.constraintName = constraintName;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> children = new ArrayList<>();
            children.add(constraintName);
            return children;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictTargetConstraint(this, ctx);
        }
    }
}

package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
import org.partiql.ast.IdentifierChain;

import java.util.ArrayList;
import java.util.List;

/**
 * This represents the potential targets for the ON CONFLICT clause.
 * @see OnConflict
 * @see OnConflict#getTarget()
 */
public abstract class ConflictTarget extends AstNode {

    /**
     * This is the index variant of the conflict target.
     *
     * @see OnConflict
     * @see ConflictTarget
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Index extends ConflictTarget {
        // TODO: Should this be a list of identifiers? Or paths? Expressions?
        @NotNull
        private final List<Identifier> indexes;

        public Index(@NotNull List<Identifier> indexes) {
            this.indexes = indexes;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            return new ArrayList<>(indexes);
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictTargetIndex(this, ctx);
        }

        @NotNull
        public List<Identifier> getIndexes() {
            return this.indexes;
        }
    }

    /**
     * This is the ON CONSTRAINT variant of the conflict target.
     *
     * @see OnConflict
     * @see ConflictTarget
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Constraint extends ConflictTarget {
        @NotNull
        private final IdentifierChain name;

        public Constraint(@NotNull IdentifierChain name) {
            this.name = name;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> children = new ArrayList<>();
            children.add(name);
            return children;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitConflictTargetConstraint(this, ctx);
        }

        @NotNull
        public IdentifierChain getName() {
            return this.name;
        }
    }
}

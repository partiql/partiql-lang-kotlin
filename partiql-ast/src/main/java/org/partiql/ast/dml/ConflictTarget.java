package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
        @Getter
        private final List<Identifier> indexes;

        /**
         * TODO
         * @param indexes TODO
         */
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
        @NotNull
        @Getter
        private final IdentifierChain name;

        /**
         * TODO
         * @param name TODO
         */
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
    }
}

package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.ExprWindowFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reference to a window.
 * @see ExprWindowFunction#getWindowReference()
 */
public abstract class WindowReference extends AstNode {
    /**
     * Represents a reference to a window by name.
     * @see ExprWindowFunction#getWindowReference()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Name extends WindowReference {
        private final Identifier.Simple name;

        /**
         * Constructs a new window reference by name.
         * @param name the name of the window reference
         */
        public Name(@NotNull Identifier.Simple name) {
            this.name = name;
        }

        /**
         * Returns the name of the window reference.
         * @return the name of the window reference
         */
        @NotNull
        public Identifier.Simple getName() {
            return this.name;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(name);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowReferenceName(this, ctx);
        }
    }

    /**
     * Represents a reference to an in-line window specification.
     * @see ExprWindowFunction#getWindowReference()
     * @see WindowSpecification
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class InLineSpecification extends WindowReference {
        private final WindowSpecification specification;

        /**
         * Constructs a new window reference by in-line specification.
         * @param specification the in-line specification of the window reference
         */
        public InLineSpecification(@NotNull WindowSpecification specification) {
            this.specification = specification;
        }

        /**
         * Returns the in-line specification of the window reference.
         * @return the in-line specification of the window reference
         */
        @NotNull
        public WindowSpecification getSpecification() {
            return this.specification;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(specification);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowReferenceInLineSpecification(this, ctx);
        }
    }
}

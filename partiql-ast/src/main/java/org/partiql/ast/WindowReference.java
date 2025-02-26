package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public abstract class WindowReference extends AstNode {
    /**
     * TODO
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Name extends WindowReference {
        private final Identifier.Simple name;

        /**
         * TODO
         * @param name TODO
         */
        public Name(@NotNull Identifier.Simple name) {
            this.name = name;
        }

        /**
         * TODO
         * @return TODO
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
     * TODO
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class InLineSpecification extends WindowReference {
        private final WindowSpecification specification;

        /**
         * TODO
         * @param specification TODO
         */
        public InLineSpecification(@NotNull WindowSpecification specification) {
            this.specification = specification;
        }

        /**
         * TODO
         * @return TODO
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

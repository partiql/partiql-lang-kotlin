package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a PartiQL WINDOW clause.
 * @see Definition
 * @see org.partiql.ast.expr.ExprWindowFunction
 * @deprecated This feature is experimental and is subject to change.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class WindowClause extends AstNode {

    @NotNull
    private final List<Definition> definitions;

    /**
     * Constructs a new WINDOW clause.
     * @param definitions the window definitions
     */
    public WindowClause(@NotNull List<Definition> definitions) {
        this.definitions = definitions;
    }

    /**
     * Returns the window definitions.
     * @return the window definitions
     */
    @NotNull
    public List<Definition> getDefinitions() {
        return this.definitions;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(definitions);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindowClause(this, ctx);
    }
    
    /**
     * Represents a window definition in a PartiQL WINDOW clause.
     * @see WindowClause
     * @deprecated This feature is experimental and is subject to change.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    @Deprecated
    public static final class Definition extends AstNode {
        @NotNull
        private final Identifier.Simple name;

        @NotNull
        private final WindowSpecification specification;

        /**
         * Constructs a new window definition.
         * @param name the window name
         * @param specification the window specification
         */
        public Definition(@NotNull Identifier.Simple name, @NotNull WindowSpecification specification) {
            this.specification = specification;
            this.name = name;
        }

        /**
         * Returns the window name.
         * @return the window name
         */
        @NotNull
        public Identifier.Simple getName() {
            return this.name;
        }

        /**
         * Returns the window specification.
         * @return the window specification
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
            kids.add(name);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowDefinition(this, ctx);
        }
    }
}

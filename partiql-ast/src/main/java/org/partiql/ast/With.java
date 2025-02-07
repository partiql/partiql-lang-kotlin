package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Represents a PartiQL WITH clause.
 * </p>
 * <p>{@code <with clause>    ::=   WITH [ RECURSIVE ] <with list>}</p>
 * <p>{@code <with list>    ::=   <with list element> [ { <comma> <with list element> }... ]}</p>
 * @see WithListElement
 * @see org.partiql.ast.expr.ExprQuerySet
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class With extends AstNode {

    @NotNull
    private final List<WithListElement> elements;

    private final boolean isRecursive;

    /**
     * Creates a new WITH clause with the specified elements and RECURSIVE set to the specified value.
     * @param elements the list of WITH list elements
     * @param isRecursive true if this WITH clause specified RECURSIVE;
     */
    public With(@NotNull List<WithListElement> elements, boolean isRecursive) {
        this.elements = elements;
        this.isRecursive = isRecursive;
    }

    /**
     * Creates a new WITH clause with the specified elements and RECURSIVE set to false.
     * @param elements the list of WITH list elements
     */
    public With(@NotNull List<WithListElement> elements) {
        this(elements, false);
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(elements);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWith(this, ctx);
    }

    /**
     * Returns the list of WITH list elements.
     * @return the list of WITH list elements
     */
    @NotNull
    public List<WithListElement> getElements() {
        return this.elements;
    }

    /**
     * Returns whether this WITH clause specified RECURSIVE.
     * @return whether this WITH clause specified RECURSIVE.
     */
    public boolean isRecursive() {
        return this.isRecursive;
    }
}

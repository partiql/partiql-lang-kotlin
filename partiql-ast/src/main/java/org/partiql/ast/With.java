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

    /**
     * TODO
     * @param elements TODO
     */
    public With(@NotNull List<WithListElement> elements) {
        this.elements = elements;
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
     * TODO
     * @return TODO
     */
    @NotNull
    public List<WithListElement> getElements() {
        return this.elements;
    }
}

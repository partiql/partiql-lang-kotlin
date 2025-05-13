package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.WindowFunctionType;
import org.partiql.ast.WindowReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a window function.
 * @see org.partiql.ast.WindowClause
 * @see ExprWindowFunction#getFunctionType()
 * @see ExprWindowFunction#getWindowReference()
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprWindowFunction extends Expr {
    @NotNull
    private final WindowFunctionType type;

    @NotNull
    private final WindowReference reference;

    /**
     * Constructs a new window function.
     * @param type the window function type
     * @param reference the window reference
     */
    public ExprWindowFunction(@NotNull WindowFunctionType type, @NotNull WindowReference reference) {
        this.type = type;
        this.reference = reference;
    }

    /**
     * Returns the window function type.
     * @return the window function type
     */
    @NotNull
    public WindowFunctionType getFunctionType() {
        return this.type;
    }

    /**
     * Returns the window reference.
     * @return the window reference
     */
    @NotNull
    public WindowReference getWindowReference() {
        return this.reference;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<AstNode>() {{
            add(type);
            add(reference);
        }};
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprWindowFunction(this, ctx);
    }
}

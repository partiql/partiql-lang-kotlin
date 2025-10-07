package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.WindowFunctionType;
import org.partiql.ast.WindowSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a window function.
 * @see org.partiql.ast.WindowClause
 * @see ExprWindowFunction#getFunctionType()
 * @see ExprWindowFunction#getWindowSpecification()
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprWindowFunction extends Expr {
    @NotNull
    private final WindowFunctionType type;

    @NotNull
    private final WindowSpecification specification;

    /**
     * Constructs a new window function.
     * @param type the window function type
     * @param specification the window reference
     */
    public ExprWindowFunction(@NotNull WindowFunctionType type, @NotNull WindowSpecification specification) {
        this.type = type;
        this.specification = specification;
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
    public WindowSpecification getWindowSpecification() {
        return this.specification;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<AstNode>() {{
            add(type);
            if (specification instanceof AstNode) {
                add((AstNode) specification);
            }
        }};
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprWindowFunction(this, ctx);
    }
}

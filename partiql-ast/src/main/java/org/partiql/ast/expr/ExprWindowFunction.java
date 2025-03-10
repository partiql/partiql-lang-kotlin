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
 * TODO
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprWindowFunction extends Expr {
    @NotNull
    private final WindowFunctionType type;

    @NotNull
    private final WindowReference reference;

    /**
     * TODO
     * @param type TODO
     * @param reference TODO
     */
    public ExprWindowFunction(@NotNull WindowFunctionType type, @NotNull WindowReference reference) {
        this.type = type;
        this.reference = reference;
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

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public WindowFunctionType getFunctionType() {
        return this.type;
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public WindowReference getWindowReference() {
        return this.reference;
    }
}

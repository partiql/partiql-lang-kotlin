package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprWindowFunction extends Expr {
    @NotNull
    private final AstNode type;

    @NotNull
    private final AstNode reference;

    /**
     * TODO
     * @param type TODO
     * @param reference TODO
     */
    public ExprWindowFunction(@NotNull AstNode type, @NotNull AstNode reference) {
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
    public AstNode getFunctionType() {
        return this.type;
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public AstNode getWindowReference() {
        return this.reference;
    }
}

package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprVariant extends Expr {
    @NotNull
    public final String value;
    @NotNull
    public final String encoding;

    public ExprVariant(@NotNull String value, @NotNull String encoding) {
        this.value = value;
        this.encoding = encoding;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprVariant(this, ctx);
    }
}

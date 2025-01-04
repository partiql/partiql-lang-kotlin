package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.DatetimeField;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's EXTRACT special form. E.g. {@code EXTRACT(YEAR FROM source)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprExtract extends Expr {
    @NotNull
    private final DatetimeField field;

    @NotNull
    private final Expr source;

    public ExprExtract(@NotNull DatetimeField field, @NotNull Expr source) {
        this.field = field;
        this.source = source;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(source);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprExtract(this, ctx);
    }

    @NotNull
    public DatetimeField getField() {
        return this.field;
    }

    @NotNull
    public Expr getSource() {
        return this.source;
    }
}

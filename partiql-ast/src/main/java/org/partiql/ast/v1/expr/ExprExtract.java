package org.partiql.ast.v1.expr;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.DatetimeField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder
public class ExprExtract extends Expr {
    @NotNull
    public final DatetimeField field;

    @NotNull
    public final Expr source;

    public ExprExtract(@NotNull DatetimeField field, @NotNull Expr source) {
        this.field = field;
        this.source = source;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(source);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprExtract(this, ctx);
    }
}

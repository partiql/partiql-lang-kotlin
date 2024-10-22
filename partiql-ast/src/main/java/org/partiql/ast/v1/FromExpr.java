package org.partiql.ast.v1;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class FromExpr extends FromTableRef {
    @NotNull
    public final Expr expr;

    @NotNull
    public final FromType fromType;

    @Nullable
    public final Identifier asAlias;

    @Nullable
    public final Identifier atAlias;

    public FromExpr(@NotNull Expr expr, @NotNull FromType fromType, @Nullable Identifier asAlias,
                    @Nullable Identifier atAlias) {
        this.expr = expr;
        this.fromType = fromType;
        this.asAlias = asAlias;
        this.atAlias = atAlias;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        if (asAlias != null) kids.add(asAlias);
        if (atAlias != null) kids.add(atAlias);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFromExpr(this, ctx);
    }
}

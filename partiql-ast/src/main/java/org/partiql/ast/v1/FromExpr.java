package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class FromExpr extends From {
    @NotNull
    public Expr expr;

    @NotNull
    public FromType fromType;

    @Nullable
    public Identifier asAlias;

    @Nullable
    public Identifier atAlias;

    /**
     * TODO get rid of `BY`
     */
    @Nullable
    public Identifier byAlias;

    public FromExpr(@NotNull Expr expr, @NotNull FromType fromType, @Nullable Identifier asAlias,
                    @Nullable Identifier atAlias, @Nullable Identifier byAlias) {
        this.expr = expr;
        this.fromType = fromType;
        this.asAlias = asAlias;
        this.atAlias = atAlias;
        this.byAlias = byAlias;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        if (asAlias != null) kids.add(asAlias);
        if (atAlias != null) kids.add(atAlias);
        if (byAlias != null) kids.add(byAlias);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFromExpr(this, ctx);
    }
}

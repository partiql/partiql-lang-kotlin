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
public class FromValue extends From {
    @NotNull
    public Expr expr;

    @NotNull
    public Type type;

    @Nullable
    public Identifier.Symbol asAlias;

    @Nullable
    public Identifier.Symbol atAlias;

    /**
     * TODO get rid of `BY`
     */
    @Nullable
    public Identifier.Symbol byAlias;

    public FromValue(@NotNull Expr expr, @NotNull Type type, @Nullable Identifier.Symbol asAlias,
    @Nullable Identifier.Symbol atAlias, @Nullable Identifier.Symbol byAlias) {
        this.expr = expr;
        this.type = type;
        this.asAlias = asAlias;
        this.atAlias = atAlias;
        this.byAlias = byAlias;
    }

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
        return visitor.visitFromValue(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Type {
        SCAN,
        UNPIVOT,
        OTHER
    }
}

package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single FROM expression.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class FromExpr extends FromTableRef {
    @NotNull
    private final Expr expr;

    @NotNull
    private final FromType fromType;

    @Nullable
    private final Identifier.Part asAlias;

    @Nullable
    private final Identifier.Part atAlias;

    public FromExpr(@NotNull Expr expr, @NotNull FromType fromType, @Nullable Identifier.Part asAlias,
                    @Nullable Identifier.Part atAlias) {
        this.expr = expr;
        this.fromType = fromType;
        this.asAlias = asAlias;
        this.atAlias = atAlias;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
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

    @NotNull
    public Expr getExpr() {
        return this.expr;
    }

    @NotNull
    public FromType getFromType() {
        return this.fromType;
    }

    @Nullable
    public Identifier.Part getAsAlias() {
        return this.asAlias;
    }

    @Nullable
    public Identifier.Part getAtAlias() {
        return this.atAlias;
    }
}

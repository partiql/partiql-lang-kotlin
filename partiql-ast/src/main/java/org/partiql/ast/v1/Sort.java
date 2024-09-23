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
public class Sort extends AstNode {
    @NotNull
    public Expr expr;

    @Nullable
    public Dir dir;

    @Nullable
    public Nulls nulls;

    public Sort(@NotNull Expr expr, @Nullable Dir dir, @Nullable Nulls nulls) {
        this.expr = expr;
        this.dir = dir;
        this.nulls = nulls;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSort(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Dir {
        ASC,
        DESC,
        OTHER
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Nulls {
        FIRST,
        LAST,
        OTHER
    }
}

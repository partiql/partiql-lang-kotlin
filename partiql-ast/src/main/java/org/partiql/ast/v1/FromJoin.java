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
public class FromJoin extends From {
    @NotNull
    public From lhs;

    @NotNull
    public From rhs;

    @Nullable
    public Type type;

    @Nullable
    public Expr condition;

    public FromJoin(@NotNull From lhs, @NotNull From rhs, @Nullable Type type, @Nullable Expr condition) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.type = type;
        this.condition = condition;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        if (condition != null) {
            kids.add(condition);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFromJoin(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Type {
        INNER,
        LEFT,
        LEFT_OUTER,
        RIGHT,
        RIGHT_OUTER,
        FULL,
        FULL_OUTER,
        CROSS,
        COMMA,
        OTHER
    }
}

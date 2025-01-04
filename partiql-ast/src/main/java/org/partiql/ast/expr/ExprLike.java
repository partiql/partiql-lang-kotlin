package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's LIKE predicate. E.g. {@code a LIKE 'a%'}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprLike extends Expr {
    @NotNull
    private final Expr value;

    @NotNull
    private final Expr pattern;

    @Nullable
    private final Expr escape;

    private final boolean not;

    public ExprLike(@NotNull Expr value, @NotNull Expr pattern, @Nullable Expr escape, boolean not) {
        this.value = value;
        this.pattern = pattern;
        this.escape = escape;
        this.not = not;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        kids.add(pattern);
        if (escape != null) {
            kids.add(escape);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprLike(this, ctx);
    }

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    @NotNull
    public Expr getPattern() {
        return this.pattern;
    }

    @Nullable
    public Expr getEscape() {
        return this.escape;
    }

    public boolean isNot() {
        return this.not;
    }
}

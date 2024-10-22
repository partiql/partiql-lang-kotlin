package org.partiql.ast.v1.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprLike extends Expr {
    @NotNull
    public final Expr value;

    @NotNull
    public final Expr pattern;

    @Nullable
    public final Expr escape;

    public final boolean not;

    public ExprLike(@NotNull Expr value, @NotNull Expr pattern, @Nullable Expr escape, boolean not) {
        this.value = value;
        this.pattern = pattern;
        this.escape = escape;
        this.not = not;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
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
}

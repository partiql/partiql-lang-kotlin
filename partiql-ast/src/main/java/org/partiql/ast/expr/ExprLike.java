package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprLike extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @NotNull
    @Getter
    private final Expr pattern;

    @Nullable
    @Getter
    private final Expr escape;

    @Getter
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
}

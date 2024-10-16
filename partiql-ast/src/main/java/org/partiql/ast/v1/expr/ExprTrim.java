package org.partiql.ast.v1.expr;

import lombok.Builder;
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
@Builder
public class ExprTrim extends Expr {
    @NotNull
    public final Expr value;

    @Nullable
    public final Expr chars;

    @Nullable
    public final TrimSpec trimSpec;

    public ExprTrim(@NotNull Expr value, @Nullable Expr chars, @Nullable TrimSpec trimSpec) {
        this.value = value;
        this.chars = chars;
        this.trimSpec = trimSpec;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        if (chars != null) {
            kids.add(chars);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprTrim(this, ctx);
    }
}

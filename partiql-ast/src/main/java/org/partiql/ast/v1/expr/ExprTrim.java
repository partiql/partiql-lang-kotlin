package org.partiql.ast.v1.expr;

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
public class ExprTrim extends Expr {
    @NotNull
    public Expr value;

    @Nullable
    public Expr chars;

    @Nullable
    public Spec spec;

    public ExprTrim(@NotNull Expr value, @Nullable Expr chars, @Nullable Spec spec) {
        this.value = value;
        this.chars = chars;
        this.spec = spec;
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

    public enum Spec {
            LEADING,
            TRAILING,
            BOTH,
            OTHER
    }
}

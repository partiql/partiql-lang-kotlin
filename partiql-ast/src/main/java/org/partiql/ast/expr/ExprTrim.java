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
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
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
    public List<AstNode> getChildren() {
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

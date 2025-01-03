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
 * Represents SQL's TRIM special form (E021-09). E.g. {@code TRIM(LEADING ' ' FROM str)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprTrim extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @Nullable
    @Getter
    private final Expr chars;

    @Nullable
    @Getter
    private final TrimSpec trimSpec;

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

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
 * Represents SQL's TRIM special form (E021-09). E.g. {@code TRIM(LEADING ' ' FROM str)}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprTrim extends Expr {
    @NotNull
    private final Expr value;

    @Nullable
    private final Expr chars;

    @Nullable
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

    @NotNull
    public Expr getValue() {
        return this.value;
    }

    @Nullable
    public Expr getChars() {
        return this.chars;
    }

    @Nullable
    public TrimSpec getTrimSpec() {
        return this.trimSpec;
    }
}

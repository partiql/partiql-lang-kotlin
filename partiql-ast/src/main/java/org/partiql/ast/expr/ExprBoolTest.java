package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL99's boolean test (6.30). E.g. {@code IS TRUE}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprBoolTest extends Expr {
    @NotNull
    @Getter
    private final Expr value;

    @Getter
    private final boolean not;

    @NotNull
    @Getter
    private final TruthValue truthValue;

    public ExprBoolTest(@NotNull Expr value, boolean not, @NotNull TruthValue truthValue) {
        this.value = value;
        this.not = not;
        this.truthValue = truthValue;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(value);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprBoolTest(this, ctx);
    }
}

package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs
 * Corresponds to SQL99's boolean test (6.30).
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprBoolTest extends Expr {
    @NotNull
    public final Expr value;

    public final boolean not;

    @NotNull
    public final TruthValue truthValue;

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

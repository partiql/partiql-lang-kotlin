package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.graph.GraphMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprMatch extends Expr {
    @NotNull
    public final Expr expr;

    @NotNull
    public final GraphMatch pattern;

    public ExprMatch(@NotNull Expr expr, @NotNull GraphMatch pattern) {
        this.expr = expr;
        this.pattern = pattern;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(expr);
        kids.add(pattern);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprMatch(this, ctx);
    }
}

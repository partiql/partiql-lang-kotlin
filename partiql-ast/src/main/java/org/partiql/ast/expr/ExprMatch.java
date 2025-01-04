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
 * Represents GPML match expression (&lt;expr&gt; MATCH &lt;pattern&gt;).
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprMatch extends Expr {
    @NotNull
    private final Expr expr;

    @NotNull
    private final GraphMatch pattern;

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

    @NotNull
    public Expr getExpr() {
        return this.expr;
    }

    @NotNull
    public GraphMatch getPattern() {
        return this.pattern;
    }
}

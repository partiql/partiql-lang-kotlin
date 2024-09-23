package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.graph.GraphMatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprMatch extends Expr {
    @NotNull
    public Expr expr;

    @NotNull
    public GraphMatch pattern;

    public ExprMatch(@NotNull Expr expr, @NotNull GraphMatch pattern) {
        this.expr = expr;
        this.pattern = pattern;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
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

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
public class ExprCase extends Expr {
    @Nullable
    public Expr expr;

    @NotNull
    public List<Branch> branches;

    @Nullable
    public Expr defaultExpr;

    public ExprCase(@Nullable Expr expr, @NotNull List<Branch> branches, @Nullable Expr defaultExpr) {
        this.expr = expr;
        this.branches = branches;
        this.defaultExpr = defaultExpr;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        if (expr != null) {
            kids.add(expr);
        }
        kids.addAll(branches);
        if (defaultExpr != null) {
            kids.add(defaultExpr);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCase(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Branch extends AstNode {
        @NotNull
        public Expr condition;

        @NotNull
        public Expr expr;

        public Branch(@NotNull Expr condition, @NotNull Expr expr) {
        this.condition = condition;
        this.expr = expr;
    }

        @Override
        @NotNull
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(condition);
            kids.add(expr);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprCaseBranch(this, ctx);
        }
    }
}

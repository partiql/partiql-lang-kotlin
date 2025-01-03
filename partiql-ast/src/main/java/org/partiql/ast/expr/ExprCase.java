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
 * Represents SQL's simple and searched CASE expression (F261-01, F261-02). E.g.
 * <code>
 * CASE
 *   WHEN expr1 THEN expr2
 *   WHEN expr3 THEN expr4
 *   ELSE expr5
 * END
 * </code>.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprCase extends Expr {
    @Nullable
    @Getter
    private final Expr expr;

    @NotNull
    @Getter
    private final List<Branch> branches;

    @Nullable
    @Getter
    private final Expr defaultExpr;

    public ExprCase(@Nullable Expr expr, @NotNull List<Branch> branches, @Nullable Expr defaultExpr) {
        this.expr = expr;
        this.branches = branches;
        this.defaultExpr = defaultExpr;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
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
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Branch extends AstNode {
        @NotNull
        @Getter
        private final Expr condition;

        @NotNull
        @Getter
        private final Expr expr;

        public Branch(@NotNull Expr condition, @NotNull Expr expr) {
        this.condition = condition;
        this.expr = expr;
    }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
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

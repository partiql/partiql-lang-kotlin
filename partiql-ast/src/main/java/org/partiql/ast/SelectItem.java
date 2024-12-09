package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class SelectItem extends AstNode {
    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Star extends SelectItem {
        @NotNull
        public final org.partiql.ast.expr.Expr expr;

        public Star(@NotNull org.partiql.ast.expr.Expr expr) {
            this.expr = expr;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(expr);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitSelectItemStar(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Expr extends SelectItem {
        @NotNull
        public final org.partiql.ast.expr.Expr expr;

        @Nullable
        public final Identifier asAlias;

        public Expr(@NotNull org.partiql.ast.expr.Expr expr, @Nullable Identifier asAlias) {
            this.expr = expr;
            this.asAlias = asAlias;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(expr);
            if (asAlias != null) {
                kids.add(asAlias);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitSelectItemExpr(this, ctx);
        }
    }
}

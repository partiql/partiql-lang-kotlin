package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an item within a select list.
 *
 * @see SelectList
 */
public abstract class SelectItem extends AstNode {
    /**
     * Represents PartiQL's {@code <select item>.*} operator used in a select list.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Star extends SelectItem {
        @NotNull
        private final org.partiql.ast.expr.Expr expr;

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

        @NotNull
        public org.partiql.ast.expr.Expr getExpr() {
            return this.expr;
        }
    }

    /**
     * Represents an expr select list item with an optional alias.
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Expr extends SelectItem {
        @NotNull
        private final org.partiql.ast.expr.Expr expr;

        @Nullable
        private final Identifier.Simple asAlias;

        public Expr(@NotNull org.partiql.ast.expr.Expr expr, @Nullable Identifier.Simple asAlias) {
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

        @NotNull
        public org.partiql.ast.expr.Expr getExpr() {
            return this.expr;
        }

        @Nullable
        public Identifier.Simple getAsAlias() {
            return this.asAlias;
        }
    }
}

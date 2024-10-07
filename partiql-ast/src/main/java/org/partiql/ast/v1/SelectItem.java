package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class SelectItem extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof Star) {
            return visitor.visitProjectItemAll((Star) this, ctx);
        } else if (this instanceof Expr) {
            return visitor.visitProjectItemExpr((Expr) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Star extends SelectItem {
        @NotNull
        public org.partiql.ast.v1.expr.Expr expr;

        public Star(@NotNull org.partiql.ast.v1.expr.Expr expr) {
            this.expr = expr;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(expr);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitProjectItemAll(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Expr extends SelectItem {
        @NotNull
        public org.partiql.ast.v1.expr.Expr expr;

        @Nullable
        public Identifier asAlias;

        public Expr(@NotNull org.partiql.ast.v1.expr.Expr expr, @Nullable Identifier asAlias) {
            this.expr = expr;
            this.asAlias = asAlias;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(expr);
            if (asAlias != null) {
                kids.add(asAlias);
            }
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitProjectItemExpr(this, ctx);
        }
    }
}

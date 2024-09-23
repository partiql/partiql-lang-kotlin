package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class ProjectItem extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof All) {
            return visitor.visitProjectItemAll((All) this, ctx);
        } else if (this instanceof Expression) {
            return visitor.visitProjectItemExpression((Expression) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class All extends ProjectItem {
        @NotNull
        public Expr expr;

        public All(@NotNull Expr expr) {
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
    public static class Expression extends ProjectItem {
        @NotNull
        public Expr expr;

        @Nullable
        public Identifier.Symbol asAlias;

        public Expression(@NotNull Expr expr, @Nullable Identifier.Symbol asAlias) {
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
            return visitor.visitProjectItemExpression(this, ctx);
        }
    }
}

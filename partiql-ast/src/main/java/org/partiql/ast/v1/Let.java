package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class Let extends AstNode {
    @NotNull
    public List<Binding> bindings;

    public Let(@NotNull List<Binding> bindings) {
        this.bindings = bindings;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>(bindings);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitLet(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Binding extends AstNode {
        @NotNull
        public Expr expr;

        @NotNull
        public Identifier asAlias;

        public Binding(@NotNull Expr expr, @NotNull Identifier asAlias) {
            this.expr = expr;
            this.asAlias = asAlias;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(expr);
            kids.add(asAlias);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitLetBinding(this, ctx);
        }
    }
}

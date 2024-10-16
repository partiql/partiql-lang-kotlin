package org.partiql.ast.v1;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder
public class Let extends AstNode {
    @NotNull
    public final List<Binding> bindings;

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
    @Builder
    public static class Binding extends AstNode {
        @NotNull
        public final Expr expr;

        @NotNull
        public final Identifier asAlias;

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

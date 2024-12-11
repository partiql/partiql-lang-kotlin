package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class Let extends AstNode {
    @NotNull
    public final List<Binding> bindings;

    public Let(@NotNull List<Binding> bindings) {
        this.bindings = bindings;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(bindings);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitLet(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
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
        public List<AstNode> getChildren() {
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

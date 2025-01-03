package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a PartiQL LET clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Let extends AstNode {
    @NotNull
    @Getter
    private final List<Binding> bindings;

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
     * Represents a binding in a PartiQL LET clause.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Binding extends AstNode {
        @NotNull
        @Getter
        private final Expr expr;

        @NotNull
        @Getter
        private final Identifier asAlias;

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

package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprCollection extends Expr {
    @NotNull
    public final Type type;

    @NotNull
    public final List<Expr> values;

    public ExprCollection(@NotNull Type type, @NotNull List<Expr> values) {
        this.type = type;
        this.values = values;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return new ArrayList<>(values);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCollection(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Type {
        BAG,
        ARRAY,
        VALUES,
        LIST,
        SEXP,
        OTHER
    }
}

package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprVarRef extends Expr {
    @NotNull
    public Identifier identifier;

    @NotNull
    public Scope scope;

    public ExprVarRef(@NotNull Identifier identifier, @NotNull Scope scope) {
        this.identifier = identifier;
        this.scope = scope;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(identifier);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprVarRef(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Scope {
        DEFAULT,
        LOCAL,
        OTHER
    }
}

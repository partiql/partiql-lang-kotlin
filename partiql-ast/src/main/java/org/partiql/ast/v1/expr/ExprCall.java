package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;
import org.partiql.ast.v1.IdentifierChain;
import org.partiql.ast.v1.SetQuantifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class ExprCall extends Expr {
    @NotNull
    public IdentifierChain function;

    @NotNull
    public List<Expr> args;

    @Nullable
    public SetQuantifier setq;

    public ExprCall(@NotNull IdentifierChain function, @NotNull List<Expr> args, @Nullable SetQuantifier setq) {
        this.function = function;
        this.args = args;
        this.setq = setq;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(function);
        kids.addAll(args);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprCall(this, ctx);
    }
}

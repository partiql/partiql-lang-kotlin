package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;
import org.partiql.ast.SetQuantifier;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprCall extends Expr {
    @NotNull
    public final IdentifierChain function;

    @NotNull
    public final List<Expr> args;

    @Nullable
    public final SetQuantifier setq;

    public ExprCall(@NotNull IdentifierChain function, @NotNull List<Expr> args, @Nullable SetQuantifier setq) {
        this.function = function;
        this.args = args;
        this.setq = setq;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
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

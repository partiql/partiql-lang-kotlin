package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprVarRef extends Expr {
    @NotNull
    public final IdentifierChain identifierChain;

    @NotNull
    public final Scope scope;

    public ExprVarRef(@NotNull IdentifierChain identifierChain, @NotNull Scope scope) {
        this.identifierChain = identifierChain;
        this.scope = scope;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(identifierChain);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprVarRef(this, ctx);
    }
}

package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the SET clause. This deviates from SQL, as we allow for paths on the LHS of the set assignment.
 * @see Update
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SetClause extends AstNode {
    // TODO: Equals and hashcode

    /**
     * TODO
     */
    @NotNull
    public final AstNode target;

    /**
     * TODO
     */
    @NotNull
    public final Expr expr;

    /**
     * TODO
     * @param target TODO
     * @param expr TODO
     */
    public SetClause(@NotNull AstNode target, @NotNull Expr expr) {
        this.target = target;
        this.expr = expr;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(target);
        kids.add(expr);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSetClause(this, ctx);
    }
}

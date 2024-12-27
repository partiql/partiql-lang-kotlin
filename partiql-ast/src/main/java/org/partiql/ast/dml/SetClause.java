package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the SET clause. This deviates from SQL, as we allow for paths on the LHS of the set assignment.
 * @see Update
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SetClause extends AstNode {
    /**
     * TODO
     */
    @NotNull
    @Getter
    private final UpdateTarget target;

    /**
     * TODO
     */
    @NotNull
    @Getter
    private final Expr expr;

    /**
     * TODO
     * @param target TODO
     * @param expr TODO
     */
    public SetClause(@NotNull UpdateTarget target, @NotNull Expr expr) {
        this.target = target;
        this.expr = expr;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
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

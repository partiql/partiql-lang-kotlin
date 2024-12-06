package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the update searched statement.
 * @see SetClause
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Update extends Statement {
    /**
     * TODO
     */
    @NotNull
    public final IdentifierChain tableName;

    /**
     * TODO
     */
    @NotNull
    public final List<SetClause> setClauses;

    /**
     * TODO
     */
    @Nullable
    public final Expr condition;

    /**
     * TODO
     * @param tableName TODO
     * @param setClauses TODO
     * @param condition TODO
     */
    public Update(@NotNull IdentifierChain tableName, @NotNull List<SetClause> setClauses, @Nullable Expr condition) {
        this.tableName = tableName;
        this.setClauses = setClauses;
        this.condition = condition;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(tableName);
        kids.addAll(setClauses);
        if (condition != null) {
            kids.add(condition);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitUpdate(this, ctx);
    }
}

package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.IdentifierChain;
import org.partiql.ast.Statement;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the update searched statement.
 *
 * @see SetClause
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Update extends Statement {
    /**
     * TODO
     */
    @NotNull
    private final IdentifierChain tableName;

    /**
     * TODO
     */
    @NotNull
    private final List<SetClause> setClauses;

    /**
     * TODO
     */
    @Nullable
    private final Expr condition;

    /**
     * TODO
     *
     * @param tableName  TODO
     * @param setClauses TODO
     * @param condition  TODO
     */
    public Update(@NotNull IdentifierChain tableName, @NotNull List<SetClause> setClauses, @Nullable Expr condition) {
        this.tableName = tableName;
        this.setClauses = setClauses;
        this.condition = condition;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
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

    @NotNull
    public IdentifierChain getTableName() {
        return this.tableName;
    }

    @NotNull
    public List<SetClause> getSetClauses() {
        return this.setClauses;
    }

    @Nullable
    public Expr getCondition() {
        return this.condition;
    }
}

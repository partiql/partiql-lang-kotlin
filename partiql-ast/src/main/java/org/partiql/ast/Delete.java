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
 * @see InsertColumnList
 * @see InsertSource
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Delete extends Statement {
    // TODO: Equals and hashcode

    /**
     * TODO
     */
    @NotNull
    public final IdentifierChain tableName;

    /**
     * TODO
     */
    @Nullable
    public final Expr condition;

    /**
     * TODO
     * @param tableName TODO
     * @param condition TODO
     */
    public Delete(@NotNull IdentifierChain tableName, @Nullable Expr condition) {
        this.tableName = tableName;
        this.condition = condition;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(tableName);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitDelete(this, ctx);
    }
}

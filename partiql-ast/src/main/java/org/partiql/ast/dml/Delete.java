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
 * This is the delete searched statement.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Delete extends Statement {
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
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(tableName);
        if (condition != null) {
            kids.add(condition);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitDelete(this, ctx);
    }
}

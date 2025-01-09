package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;
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
    @NotNull
    private final Identifier tableName;

    @Nullable
    private final Expr condition;

    public Delete(@NotNull Identifier tableName, @Nullable Expr condition) {
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

    @NotNull
    public Identifier getTableName() {
        return this.tableName;
    }

    @Nullable
    public Expr getCondition() {
        return this.condition;
    }
}

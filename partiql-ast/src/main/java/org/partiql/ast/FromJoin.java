package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a join in a FROM clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class FromJoin extends FromTableRef {
    @NotNull
    @Getter
    private final FromTableRef lhs;

    @NotNull
    @Getter
    private final FromTableRef rhs;

    @Nullable
    @Getter
    private final JoinType joinType;

    @Nullable
    @Getter
    private final Expr condition;

    public FromJoin(@NotNull FromTableRef lhs, @NotNull FromTableRef rhs, @Nullable JoinType joinType, @Nullable Expr condition) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.joinType = joinType;
        this.condition = condition;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
        if (joinType != null) {
            kids.add(joinType);
        }
        if (condition != null) {
            kids.add(condition);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFromJoin(this, ctx);
    }
}

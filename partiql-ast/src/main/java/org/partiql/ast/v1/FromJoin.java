package org.partiql.ast.v1;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.expr.Expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
public class FromJoin extends FromTableRef {
    @NotNull
    public final From lhs;

    @NotNull
    public final From rhs;

    @Nullable
    public final JoinType joinType;

    @Nullable
    public final Expr condition;

    public FromJoin(@NotNull From lhs, @NotNull From rhs, @Nullable JoinType joinType, @Nullable Expr condition) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.joinType = joinType;
        this.condition = condition;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(lhs);
        kids.add(rhs);
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

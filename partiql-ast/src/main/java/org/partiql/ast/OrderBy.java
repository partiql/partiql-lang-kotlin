package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class OrderBy extends AstNode {
    @NotNull
    @Getter
    private final List<Sort> sorts;

    public OrderBy(@NotNull List<Sort> sorts) {
        this.sorts = sorts;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(sorts);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitOrderBy(this, ctx);
    }
}

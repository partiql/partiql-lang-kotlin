package org.partiql.ast.v1;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class OrderBy extends AstNode {
    @NotNull
    public final List<Sort> sorts;

    public OrderBy(@NotNull List<Sort> sorts) {
        this.sorts = sorts;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>(sorts);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitOrderBy(this, ctx);
    }
}

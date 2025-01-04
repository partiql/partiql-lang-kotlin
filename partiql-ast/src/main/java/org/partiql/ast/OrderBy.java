package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's ORDER BY clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class OrderBy extends AstNode {
    @NotNull
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

    @NotNull
    public List<Sort> getSorts() {
        return this.sorts;
    }
}

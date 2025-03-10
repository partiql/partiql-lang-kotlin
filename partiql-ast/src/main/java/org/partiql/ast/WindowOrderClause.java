package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class WindowOrderClause extends AstNode {
    @NotNull
    private final List<Sort> sorts;

    /**
     * TODO
     * @param sorts TODO
     */
    public WindowOrderClause(@NotNull List<Sort> sorts) {
        this.sorts = sorts;
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public List<Sort> getSorts() {
        return this.sorts;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(sorts);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindowOrderClause(this, ctx);
    }
}

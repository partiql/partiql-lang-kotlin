package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a FROM clause in a PartiQL query.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class From extends AstNode {
    @NotNull
    private final List<FromTableRef> tableRefs;

    public From(@NotNull List<FromTableRef> tableRefs) {
        this.tableRefs = tableRefs;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(tableRefs);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFrom(this, ctx);
    }

    @NotNull
    public List<FromTableRef> getTableRefs() {
        return this.tableRefs;
    }
}

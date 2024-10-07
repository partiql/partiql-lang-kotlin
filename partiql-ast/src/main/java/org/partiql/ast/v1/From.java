package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class From extends AstNode {
    @NotNull
    public List<FromTableRef> tableRefs;

    public From(@NotNull List<FromTableRef> tableRefs) {
        this.tableRefs = tableRefs;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>(tableRefs);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitFrom(this, ctx);
    }
}

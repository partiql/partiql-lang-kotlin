package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class TableDefinition extends AstNode {
    @NotNull
    public List<Column> columns;

    public TableDefinition(@NotNull List<Column> columns) {
        this.columns = columns;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>(columns);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitTableDefinition(this, ctx);
    }
}

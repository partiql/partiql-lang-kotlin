package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This specifies the list of columns to insert into.
 * @see Insert
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class InsertColumnList extends Statement {
    // TODO: Equals and hashcode

    @NotNull
    public final List<Identifier> columns;

    public InsertColumnList(@NotNull List<Identifier> columns) {
        this.columns = columns;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>(columns);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitInsertColumnList(this, ctx);
    }
}

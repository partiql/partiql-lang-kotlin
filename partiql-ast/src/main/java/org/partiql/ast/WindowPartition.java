package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.Expr;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a partition specified by a window specification.
 * @see WindowSpecification#getPartitionClause()
 * @deprecated This feature is experimental and is subject to change.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class WindowPartition extends AstNode {

    private final Expr columnReference;

    /**
     * Constructs a new window partition backed by a column reference.
     * @param columnReference the column reference backing the window partition
     */
    public WindowPartition(@NotNull Expr columnReference) {
        this.columnReference = columnReference;
    }

    /**
     * Returns the column reference backing the window partition.
     * @return the column reference backing the window partition
     */
    @NotNull
    public Expr getColumnReference() {
        return this.columnReference;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(columnReference);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindowPartition(this, ctx);
    }
}

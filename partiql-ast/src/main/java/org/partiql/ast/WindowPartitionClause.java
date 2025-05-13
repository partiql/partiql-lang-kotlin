package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a window specification's partition by clause.
 * @see WindowSpecification#getPartitionClause()
 * @see WindowPartition
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class WindowPartitionClause extends AstNode {
    @NotNull
    private final List<WindowPartition> partitions;

    /**
     * Constructs a new window partition clause.
     * @param partitions the partitions of the window partition clause
     */
    public WindowPartitionClause(@NotNull List<WindowPartition> partitions) {
        this.partitions = partitions;
    }

    /**
     * Returns the partitions of the window partition clause.
     * @return the partitions of the window partition clause
     */
    @NotNull
    public List<WindowPartition> getPartitions() {
        return this.partitions;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>(partitions);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindowPartitionClause(this, ctx);
    }
}

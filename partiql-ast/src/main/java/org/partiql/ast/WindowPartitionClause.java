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
public final class WindowPartitionClause extends AstNode {
    @NotNull
    private final List<WindowPartition> partitions;

    /**
     * TODO
     * @param partitions TODO
     */
    public WindowPartitionClause(@NotNull List<WindowPartition> partitions) {
        this.partitions = partitions;
    }

    /**
     * TODO
     * @return TODO
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

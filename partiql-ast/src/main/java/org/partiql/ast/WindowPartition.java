package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a partition specified by a window specification.
 * @see Name
 * @see WindowPartitionClause#getPartitions()
 * @see WindowSpecification#getPartitionClause()
 */
public abstract class WindowPartition extends AstNode {
    /**
     * Represents a partition specified by a name.
     * @see WindowPartitionClause#getPartitions()
     * @see WindowSpecification#getPartitionClause()
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Name extends WindowPartition {
        private final Identifier columnReference;

        /**
         * Constructs a new window partition backed by a column reference.
         * @param columnReference the column reference backing the window partition
         */
        public Name(@NotNull Identifier columnReference) {
            this.columnReference = columnReference;
        }

        /**
         * Returns the column reference backing the window partition.
         * @return the column reference backing the window partition
         */
        @NotNull
        public Identifier getColumnReference() {
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
            return visitor.visitWindowPartitionName(this, ctx);
        }
    }
}

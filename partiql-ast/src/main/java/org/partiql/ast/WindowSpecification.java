package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a window specification.
 * @see WindowClause.WindowDefinition#getSpecification()
 * @see WindowReference.InLineSpecification
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class WindowSpecification extends AstNode {
    // TODO: In the future: @Nullable private final WindowFrameClause frameClause;

    @Nullable
    private final Identifier.Simple existingName;

    @Nullable
    private final WindowPartitionClause partitionClause;

    @Nullable
    private final WindowOrderClause orderClause;

    /**
     * Constructs a new window specification.
     * @param existingName the existing name of the window specification
     * @param partitionClause the partition clause of the window specification
     * @param orderClause the order clause of the window specification
     */
    public WindowSpecification(
            @Nullable Identifier.Simple existingName,
            @Nullable WindowPartitionClause partitionClause,
            @Nullable WindowOrderClause orderClause
    ) {
        this.existingName = existingName;
        this.partitionClause = partitionClause;
        this.orderClause = orderClause;
    }

    /**
     * Returns the existing name of the window specification.
     * @return the existing name of the window specification
     */
    @Nullable
    public Identifier.Simple getExistingName() {
        return this.existingName;
    }

    /**
     * Returns the partition clause of the window specification.
     * @return the partition clause of the window specification
     */
    @Nullable
    public WindowPartitionClause getPartitionClause() {
        return this.partitionClause;
    }

    /**
     * Returns the order clause of the window specification.
     * @return the order clause of the window specification
     */
    @Nullable
    public WindowOrderClause getOrderClause() {
        return this.orderClause;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindowSpecification(this, ctx);
    }
}

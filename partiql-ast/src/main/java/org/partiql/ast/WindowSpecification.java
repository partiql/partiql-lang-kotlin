package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
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
     * TODO
     * @param existingName TODO
     * @param partitionClause TODO
     * @param orderClause TODO
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
     * TODO
     * @return TODO
     */
    @Nullable
    public Identifier.Simple getExistingName() {
        return this.existingName;
    }

    /**
     * TODO
     * @return TODO
     */
    @Nullable
    public WindowPartitionClause getPartitionClause() {
        return this.partitionClause;
    }

    /**
     * TODO
     * @return TODO
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

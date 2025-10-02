package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a window specification or a window reference.
 * @see WindowClause.Definition#getSpecification()
 * @deprecated This feature is experimental and is subject to change.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
@Deprecated
public final class WindowSpecification extends AstNode {
    // TODO: In the future: @Nullable private final WindowFrameClause frameClause;
    //  See: https://github.com/partiql/partiql-lang-kotlin/issues/1837

    @Nullable
    private final Identifier.Simple existingName;

    @Nullable
    private final List<WindowPartition> partitionClause;

    @Nullable
    private final OrderBy orderClause;

    /**
     * Constructs a new window specification.
     * @param existingName the existing name of the window specification
     * @param partitionClause the partition clause of the window specification
     * @param orderClause the order clause of the window specification
     */
    public WindowSpecification(
            @Nullable Identifier.Simple existingName,
            @Nullable List<WindowPartition> partitionClause,
            @Nullable OrderBy orderClause
    ) {
        this.existingName = existingName;
        this.partitionClause = partitionClause;
        this.orderClause = orderClause;
    }

    /**
     * Returns the name of an existing window specification.
     * @return the name of an existing window specification
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
    public List<WindowPartition> getPartitionClause() {
        return this.partitionClause;
    }

    /**
     * Returns the order clause of the window specification.
     * @return the order clause of the window specification
     */
    @Nullable
    public OrderBy getOrderClause() {
        return this.orderClause;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> children = new ArrayList<>();
        if (this.existingName != null) {
            children.add(this.existingName);
        }
        if (this.partitionClause != null) {
            children.addAll(this.partitionClause);
        }
        if (this.orderClause != null) {
            children.add(this.orderClause);
        }
        return children;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindowSpecification(this, ctx);
    }
}

package org.partiql.ast.graph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Graph edge quantifier. E.g., the {@code {2,5}} in {@code MATCH (x)->{2,5}(y)}).
 * <p>
 * Note: this is an experimental API and subject to change without prior notice.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class GraphQuantifier extends AstNode {
    private final long lower;

    @Nullable
    private final Long upper;

    public GraphQuantifier(long lower, @Nullable Long upper) {
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitGraphQuantifier(this, ctx);
    }

    public long getLower() {
        return this.lower;
    }

    @Nullable
    public Long getUpper() {
        return this.upper;
    }
}

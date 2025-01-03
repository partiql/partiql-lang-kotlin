package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's EXCLUDE clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Exclude extends AstNode {
    @NotNull
    @Getter
    private final List<ExcludePath> excludePaths;

    public Exclude(@NotNull List<ExcludePath> excludePaths) {
        this.excludePaths = excludePaths;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(excludePaths);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExclude(this, ctx);
    }
}

package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Represents SQL's SELECT * clause.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SelectStar extends Select {
    @Nullable
    private final SetQuantifier setq;

    public SelectStar(@Nullable SetQuantifier setq) {
        this.setq = setq;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectStar(this, ctx);
    }

    @Nullable
    public SetQuantifier getSetq() {
        return this.setq;
    }
}

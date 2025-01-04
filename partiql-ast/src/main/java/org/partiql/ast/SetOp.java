package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a set operation including its set op type and set quantifier.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SetOp extends AstNode {
    @NotNull
    private final SetOpType setOpType;

    @Nullable
    private final SetQuantifier setq;

    public SetOp(@NotNull SetOpType setOpType, @Nullable SetQuantifier setq) {
        this.setOpType = setOpType;
        this.setq = setq;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSetOp(this, ctx);
    }

    @NotNull
    public SetOpType getSetOpType() {
        return this.setOpType;
    }

    @Nullable
    public SetQuantifier getSetq() {
        return this.setq;
    }
}

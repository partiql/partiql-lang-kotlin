package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class SetOp extends AstNode {
    @NotNull
    public final SetOpType setOpType;

    @Nullable
    public final SetQuantifier setq;

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
}

package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public class SetOp extends AstNode {
    @NotNull
    public Type type;

    @Nullable
    public SetQuantifier setq;

    public SetOp(@NotNull Type type, @Nullable SetQuantifier setq) {
        this.type = type;
        this.setq = setq;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSetOp(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Type {
        UNION,
        INTERSECT,
        EXCEPT,
        OTHER
    }
}

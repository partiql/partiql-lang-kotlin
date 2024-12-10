package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class SelectList extends Select {
    @NotNull
    public final List<SelectItem> items;

    @Nullable
    public final SetQuantifier setq;

    public SelectList(@NotNull List<SelectItem> items, @Nullable SetQuantifier setq) {
        this.items = items;
        this.setq = setq;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(items);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectList(this, ctx);
    }
}

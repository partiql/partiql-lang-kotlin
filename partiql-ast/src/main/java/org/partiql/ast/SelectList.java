package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SQL's SELECT list clause.
 *
 * @see SelectItem
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class SelectList extends Select {
    @NotNull
    private final List<SelectItem> items;

    @Nullable
    private final SetQuantifier setq;

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

    @NotNull
    public List<SelectItem> getItems() {
        return this.items;
    }

    @Nullable
    public SetQuantifier getSetq() {
        return this.setq;
    }
}

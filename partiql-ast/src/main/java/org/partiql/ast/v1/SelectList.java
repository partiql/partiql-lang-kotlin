package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class SelectList extends Select {
    @NotNull
    public List<SelectItem> items;

    @Nullable
    public SetQuantifier setq;

    public SelectList(@NotNull List<SelectItem> items, @Nullable SetQuantifier setq) {
        this.items = items;
        this.setq = setq;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>(items);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitSelectList(this, ctx);
    }
}

package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class OrderBy extends AstNode {
    @NotNull
    public List<Sort> sorts;

    public OrderBy(@NotNull List<Sort> sorts) {
        this.sorts = sorts;
    }

    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.addAll(sorts);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitOrderBy(this, ctx);
    }
}

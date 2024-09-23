package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class DropIndex extends DDL {
    @NotNull
    public Identifier index;

    @NotNull
    public Identifier table;

    public DropIndex(@NotNull Identifier index, @NotNull Identifier table) {
        this.index = index;
        this.table = table;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(index);
        kids.add(table);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
    return visitor.visitDropIndex(this, ctx);
}
}

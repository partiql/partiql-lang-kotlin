package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class CreateIndex extends DDL {
    @Nullable
    public Identifier index;

    @NotNull
    public Identifier table;

    @NotNull
    public List<PathLit> fields;

    public CreateIndex(@Nullable Identifier index, @NotNull Identifier table, @NotNull List<PathLit> fields) {
        this.index = index;
        this.table = table;
        this.fields = fields;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        if (index != null) {
            kids.add(index);
        }
        kids.add(table);
        kids.addAll(fields);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitCreateIndex(this, ctx);
    }
}

package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class CreateTable extends DDL {
    @NotNull
    public Identifier name;

    @Nullable
    public TableDefinition definition;

    public CreateTable(@NotNull Identifier name, @Nullable TableDefinition definition) {
        this.name = name;
        this.definition = definition;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(name);
        if (definition != null) {
            kids.add(definition);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitCreateTable(this, ctx);
    }
}

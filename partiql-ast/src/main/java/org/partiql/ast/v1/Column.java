package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.type.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public class Column extends AstNode {
    @NotNull
    public String name;

    @NotNull
    public Type type;

    @NotNull
    public List<Constraint> constraints;

    public Column(@NotNull String name, @NotNull Type type, @NotNull List<Constraint> constraints) {
        this.name = name;
        this.type = type;
        this.constraints = constraints;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(type);
        kids.addAll(constraints);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitColumn(this, ctx);
    }
}

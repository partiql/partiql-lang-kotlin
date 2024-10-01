package org.partiql.ast.v1.type;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public class TypeUserDefined extends Type {
    @NotNull
    public String name;

    public TypeUserDefined(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitTypeUserDefined(this, ctx);
    }
}

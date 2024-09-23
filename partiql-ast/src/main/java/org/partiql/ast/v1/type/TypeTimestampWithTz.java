package org.partiql.ast.v1.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public class TypeTimestampWithTz extends Type {
    @Nullable
    public Integer precision;

    public TypeTimestampWithTz(@Nullable Integer precision) {
        this.precision = precision;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitTypeTimestampWithTz(this, ctx);
    }
}

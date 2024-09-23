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
public class TypeNumeric extends Type {
    @Nullable
    public Integer precision;

    @Nullable
    public Integer scale;

    public TypeNumeric(@Nullable Integer precision, @Nullable Integer scale) {
        this.precision = precision;
        this.scale = scale;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitTypeNumeric(this, ctx);
    }
}

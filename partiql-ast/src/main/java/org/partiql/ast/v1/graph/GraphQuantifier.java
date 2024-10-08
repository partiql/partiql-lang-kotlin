package org.partiql.ast.v1.graph;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public class GraphQuantifier extends AstNode {
    @NotNull
    public Long lower;

    @Nullable
    public Long upper;

    public GraphQuantifier(@NotNull Long lower, @Nullable Long upper) {
        this.lower = lower;
        this.upper = upper;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitGraphQuantifier(this, ctx);
    }
}

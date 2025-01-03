package org.partiql.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base abstract class for the AST nodes.
 */
public abstract class AstNode {
    private int tag = 0;

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    @NotNull
    public abstract List<AstNode> getChildren();

    public abstract <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}

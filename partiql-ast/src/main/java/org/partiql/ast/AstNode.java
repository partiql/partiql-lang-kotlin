package org.partiql.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Abstract base class for the AST nodes.
 */
public abstract class AstNode {
    private int tag = 0;

    /**
     * @return the tag for this node.
     */
    public int getTag() {
        return tag;
    }

    /**
     * Sets the tag for this node.
     * @param tag the tag to be set.
     */
    public void setTag(int tag) {
        this.tag = tag;
    }

    /**
     * @return child AST nodes of this node.
     */
    @NotNull
    public abstract List<AstNode> getChildren();

    /**
     * Accepts a generic visitor.
     */
    public abstract <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}

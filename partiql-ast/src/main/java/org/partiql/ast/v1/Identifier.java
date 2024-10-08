package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO docs, equals, hashcode
 */
public class Identifier extends AstNode {
    @NotNull
    public String symbol;

    public boolean isDelimited;

    public Identifier(@NotNull String symbol, boolean isDelimited) {
        this.symbol = symbol;
        this.isDelimited = isDelimited;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        return new ArrayList<>();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitIdentifier(this, ctx);
    }
}

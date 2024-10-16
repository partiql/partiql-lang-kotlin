package org.partiql.ast.v1;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO docs, equals, hashcode
 */
@Builder
public class Identifier extends AstNode {
    @NotNull
    public final String symbol;

    public final boolean isDelimited;

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

package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an identifier in PartiQL, which may be delimited or not.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Identifier extends AstNode {
    @NotNull
    private final String symbol;

    private final boolean delimited;

    public Identifier(@NotNull String symbol, boolean delimited) {
        this.symbol = symbol;
        this.delimited = delimited;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitIdentifier(this, ctx);
    }

    @NotNull
    public String getSymbol() {
        return this.symbol;
    }

    public boolean isDelimited() {
        return this.delimited;
    }
}

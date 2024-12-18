package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
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
    public List<AstNode> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitIdentifier(this, ctx);
    }
}

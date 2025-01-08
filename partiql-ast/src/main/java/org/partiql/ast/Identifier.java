package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SQL identifier, which may be qualified, such as {@code foo.bar.baz}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class Identifier extends AstNode {
    @NotNull
    private final List<Part> qualifier;

    @NotNull
    private final Part base;

    public Identifier(@NotNull List<Part> qualifier, @NotNull Part base) {
        this.qualifier = qualifier;
        this.base = base;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.addAll(qualifier);
        kids.add(base);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitIdentifier(this, ctx);
    }

    @NotNull
    public List<Part> getQualifier() {
        return this.qualifier;
    }

    @NotNull
    public Part getBase() {
        return this.base;
    }

    /**
     * Represents a part of a SQL identifier, such as {@code c} in {@code a.b.c}.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Part extends AstNode {
        @NotNull
        private final String symbol;

        private final boolean delimited;

        public Part(@NotNull String symbol, boolean delimited) {
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
            return visitor.visitIdentifierPart(this, ctx);
        }

        @NotNull
        public String getSymbol() {
            return this.symbol;
        }

        public boolean isDelimited() {
            return this.delimited;
        }
    }
}

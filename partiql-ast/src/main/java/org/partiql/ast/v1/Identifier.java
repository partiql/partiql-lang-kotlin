package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Identifier extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof Symbol) {
            return visitor.visitIdentifierSymbol((Symbol) this, ctx);
        } else if (this instanceof Qualified) {
            return visitor.visitIdentifierQualified((Qualified) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    /**
     * TODO docs, equals, hashcode; TODO change to other name pending comment followup to
     * https://github.com/partiql/partiql-lang-kotlin/pull/1579#discussion_r1775983916
     *
     */
    public static class Symbol extends Identifier {
        @NotNull
        public String symbol;

        public boolean isDelimited;

        public Symbol(@NotNull String symbol, boolean isDelimited) {
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
            return visitor.visitIdentifierSymbol(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Qualified extends Identifier {
        @NotNull
        public Symbol root;

        @NotNull
        public List<Symbol> steps;

        public Qualified(@NotNull Symbol root, @NotNull List<Symbol> steps) {
            this.root = root;
            this.steps = steps;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(root);
            kids.addAll(steps);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitIdentifierQualified(this, ctx);
        }
    }
}

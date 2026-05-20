package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents PartiQL's map constructor. E.g. {@code MAP {'a': 1, 'b': 2, 'c': 3}}.
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprMap extends Expr {
    @NotNull
    private final List<Entry> entries;

    public ExprMap(@NotNull List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return new ArrayList<>(entries);
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprMap(this, ctx);
    }

    @NotNull
    public List<Entry> getEntries() {
        return this.entries;
    }

    /**
     * Represents a single entry of a map constructor. E.g. {@code 'a': 1}.
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static class Entry extends AstNode {
        @NotNull
        private final Expr key;

        @NotNull
        private final Expr value;

        public Entry(@NotNull Expr key, @NotNull Expr value) {
            this.key = key;
            this.value = value;
        }

        @Override
        @NotNull
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(key);
            kids.add(value);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitExprMapEntry(this, ctx);
        }

        @NotNull
        public Expr getKey() {
            return this.key;
        }

        @NotNull
        public Expr getValue() {
            return this.value;
        }
    }
}

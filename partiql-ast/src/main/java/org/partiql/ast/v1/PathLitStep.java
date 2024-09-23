package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
public abstract class PathLitStep extends AstNode {
    /**
     * TODO docs, equals, hashcode
     */
    public static class Symbol extends PathLitStep {
        @NotNull
        public Identifier.Symbol symbol;

        public Symbol(@NotNull Identifier.Symbol symbol) {
            this.symbol = symbol;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(symbol);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathLitStepSymbol(this, ctx);
        }
    }

    /**
     * TODO docs, equals, hashcode
     */
    public static class Index extends PathLitStep {
        public int index;

        public Index(int index) {
            this.index = index;
        }

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitPathLitStepIndex(this, ctx);
        }
    }
}

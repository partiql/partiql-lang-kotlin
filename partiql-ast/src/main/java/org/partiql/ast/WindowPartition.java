package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public abstract class WindowPartition extends AstNode {
    /**
     * TODO
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Name extends WindowPartition {
        private final Identifier columnReference;

        /**
         * TODO
         * @param columnReference TODO
         */
        public Name(@NotNull Identifier columnReference) {
            this.columnReference = columnReference;
        }

        /**
         * TODO
         * @return TODO
         */
        @NotNull
        public Identifier getName() {
            return this.columnReference;
        }

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(columnReference);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitWindowPartitionName(this, ctx);
        }
    }
}

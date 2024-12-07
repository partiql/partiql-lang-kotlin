package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This represents the potential actions after the DO REPLACE clause. While there are more variants beyond EXCLUDED,
 * only EXCLUDED is currently implemented.
 * @see ConflictAction.DoReplace
 * @see OnConflict#action
 */
public abstract class DoReplaceAction extends AstNode {
    // TODO: There are more variants than just EXCLUDED. See the Javadoc above.

    /**
     * This is the EXCLUDED variant of the DO REPLACE clause.
     * @see ConflictAction.DoReplace
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Excluded extends DoReplaceAction {
        /**
         * TODO
         */
        public Excluded() {}

        @NotNull
        @Override
        public Collection<AstNode> children() {
            return new ArrayList<>();
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitDoReplaceActionExcluded(this, ctx);
        }
    }
}

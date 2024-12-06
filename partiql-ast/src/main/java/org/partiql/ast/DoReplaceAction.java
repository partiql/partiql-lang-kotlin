package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This represents the potential actions after the DO REPLACE clause.
 * @see ConflictAction.DoReplace
 * @see OnConflict#action
 */
public abstract class DoReplaceAction extends AstNode {

    /**
     * This is the EXCLUDED variant of the DO REPLACE clause.
     * @see ConflictAction.DoReplace
     */
    @Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Excluded extends DoReplaceAction {
        // TODO: Equals and hashcode

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

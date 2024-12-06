package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the ON CONFLICT clause for the INSERT statement.
 * @see Insert
 * @see Insert#onConflict
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class OnConflict extends AstNode {
    /**
     * TODO
     */
    @NotNull
    public final ConflictAction action;

    /**
     * TODO
     */
    @Nullable
    public final ConflictTarget target;

    /**
     * TODO
     * @param action TODO
     * @param target TODO
     */
    public OnConflict(@NotNull ConflictAction action, @Nullable ConflictTarget target) {
        this.action = action;
        this.target = target;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(action);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitOnConflict(this, ctx);
    }
}

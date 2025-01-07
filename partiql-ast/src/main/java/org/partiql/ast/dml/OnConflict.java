package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the ON CONFLICT clause for the INSERT statement.
 *
 * @see Insert
 * @see Insert#getOnConflict()
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class OnConflict extends AstNode {
    @NotNull
    private final ConflictAction action;

    @Nullable
    private final ConflictTarget target;

    public OnConflict(@NotNull ConflictAction action, @Nullable ConflictTarget target) {
        this.action = action;
        this.target = target;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(action);
        if (target != null) {
            kids.add(target);
        }
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitOnConflict(this, ctx);
    }

    @NotNull
    public ConflictAction getAction() {
        return this.action;
    }

    @Nullable
    public ConflictTarget getTarget() {
        return this.target;
    }
}

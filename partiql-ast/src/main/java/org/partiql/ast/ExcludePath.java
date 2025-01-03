package org.partiql.ast;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.expr.ExprVarRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single exclude path in an exclude clause. Composed on one or more exclude steps.
 *
 * @see Exclude
 * @see ExcludeStep
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExcludePath extends AstNode {
    @NotNull
    @Getter
    private final ExprVarRef root;

    @NotNull
    @Getter
    private final List<ExcludeStep> excludeSteps;

    public ExcludePath(@NotNull ExprVarRef root, @NotNull List<ExcludeStep> excludeSteps) {
        this.root = root;
        this.excludeSteps = excludeSteps;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        kids.addAll(excludeSteps);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
    return visitor.visitExcludePath(this, ctx);
}
}

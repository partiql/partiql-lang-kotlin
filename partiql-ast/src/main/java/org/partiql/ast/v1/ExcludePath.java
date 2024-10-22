package org.partiql.ast.v1;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.expr.ExprVarRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExcludePath extends AstNode {
    @NotNull
    public final ExprVarRef root;

    @NotNull
    public final List<ExcludeStep> excludeSteps;

    public ExcludePath(@NotNull ExprVarRef root, @NotNull List<ExcludeStep> excludeSteps) {
        this.root = root;
        this.excludeSteps = excludeSteps;
    }

    @NotNull
    @Override
    public Collection<AstNode> children() {
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

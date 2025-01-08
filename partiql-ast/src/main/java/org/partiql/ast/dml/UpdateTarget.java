package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * This references a column or column's nested properties. In SQL:1999, the EBNF rule is &lt;update target&gt;.
 * This implementation differs from SQL by allowing for references to deeply nested data of varying types.
 *
 * @see SetClause
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class UpdateTarget extends AstNode {
    @NotNull
    private final Identifier.Simple root;

    @NotNull
    private final List<UpdateTargetStep> steps;

    public UpdateTarget(@NotNull Identifier.Simple root, @NotNull List<UpdateTargetStep> steps) {
        this.root = root;
        this.steps = steps;
    }

    @NotNull
    @Override
    public List<AstNode> getChildren() {
        List<AstNode> kids = new ArrayList<>();
        kids.add(root);
        kids.addAll(steps);
        return kids;
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitUpdateTarget(this, ctx);
    }

    @NotNull
    public Identifier.Simple getRoot() {
        return this.root;
    }

    @NotNull
    public List<UpdateTargetStep> getSteps() {
        return this.steps;
    }
}

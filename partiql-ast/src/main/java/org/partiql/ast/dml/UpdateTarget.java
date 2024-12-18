package org.partiql.ast.dml;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;
import org.partiql.ast.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * This references a column or column's nested properties. In SQL:1999, the EBNF rule is &lt;update target&gt;.
 * This implementation differs from SQL by allowing for references to deeply nested data of varying types.
 * @see SetClause
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class UpdateTarget extends AstNode {
    /**
     * TODO
     */
    @NotNull
    @Getter
    private final Identifier root;

    /**
     * TODO
     */
    @NotNull
    @Getter
    private final List<UpdateTargetStep> steps;

    /**
     * TODO
     * @param root TODO
     * @param steps TODO
     */
    public UpdateTarget(@NotNull Identifier root, @NotNull List<UpdateTargetStep> steps) {
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
}

package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

/**
 * Logical filter operation for the WHERE and HAVING clauses.
 * <br>
 * <br>
 * <ol>
 *     <li>input (rel)</li>
 *     <li>predicate (rex)</li>
 * </ol>
 */
public interface RelFilter extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Rex getPredicate();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitFilter(this, ctx);
    }
}

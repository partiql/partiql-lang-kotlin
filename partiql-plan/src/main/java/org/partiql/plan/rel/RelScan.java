package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

/**
 * Logical scan corresponding to the clause `FROM <expression> AS <v>`.
 */
public interface RelScan extends Rel {

    @NotNull
    public Rex getRex();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitScan(this, ctx);
    }
}

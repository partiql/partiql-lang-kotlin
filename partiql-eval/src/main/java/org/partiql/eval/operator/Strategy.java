package org.partiql.eval.operator;

import org.partiql.plan.Operator;

/**
 * Converts a logical plan operator into a physical operator (operation).
 */
public interface Strategy {

    /**
     * Applies the strategy to a logical plan operator and returns the physical operation.
     *
     * @param operator the logical operator to be converted
     * @return the physical operation
     */
    public Operation apply(Operator operator);
}

package org.partiql.eval.operator;

import org.partiql.eval.Environment;
import org.partiql.spi.value.Datum;

/**
 * PhysicalExpr is the interface for an operator that returns a value.
 */
public interface Expression extends PhysicalOperator {

    /**
     * Evaluate the expression for the given environment.
     * @param env   The current environment.
     * @return      The expression result.
     */
    public Datum eval(Environment env);
}

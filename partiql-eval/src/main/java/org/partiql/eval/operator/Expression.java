package org.partiql.eval.operator;

import org.partiql.spi.value.Datum;

/**
 * Expression is the interface for an operator that returns a value.
 */
public interface Expression extends Operation {

    /**
     * Evaluates the expression and returns the result.
     * @return the result of the expression
     */
    public Datum eval();
}

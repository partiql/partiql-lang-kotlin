package org.partiql.eval;

import org.partiql.spi.value.Datum;

/**
 * ExprValue is the interface for an expression (physical operator) that returns a value.
 */
public interface ExprValue extends Expr {

    /**
     * Evaluate the expression for the given environment.
     *
     * @param env   The current environment.
     * @return      The expression result.
     */
    public Datum eval(Environment env);
}

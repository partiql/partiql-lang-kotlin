package org.partiql.eval;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * ExprRelation is the interface for a expression which returns a "collection of binding tuples" aka iterator of rows.
 */
public interface ExprRelation extends Expr, AutoCloseable, Iterator<Row> {

    /**
     * Prepares all resources for execution. Resets any modified state.
     * @param env the environment to use for evaluation
     */
    public void open(@NotNull Environment env);

    @NotNull
    public Row next();

    /**
     * Returns true if there are more rows to be returned.
     * @return true if there are more rows to be returned
     */
    public boolean hasNext();

    /**
     * Closes and resets all resources used for execution.
     */
    public void close();

    @Override
    default void remove() {
        Iterator.super.remove();
    }
}

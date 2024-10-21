package org.partiql.eval;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * ExprRelation is the interface for a expression which returns a "collection of binding tuples" aka iterator of rows.
 */
public interface ExprRelation extends Expr, AutoCloseable, Iterator<Row> {

    public void open(@NotNull Environment env);

    @NotNull
    public Row next();

    public boolean hasNext();

    public void close();

    @Override
    default void remove() {
        Iterator.super.remove();
    }
}

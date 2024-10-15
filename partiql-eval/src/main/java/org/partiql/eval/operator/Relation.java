package org.partiql.eval.operator;

import org.jetbrains.annotations.NotNull;
import org.partiql.eval.Environment;

import java.util.Iterator;

/**
 * Relation is the interface for an operator that returns a relation.
 */
public interface Relation extends PhysicalOperator, AutoCloseable, Iterator<Record> {

    public void open(@NotNull Environment env);

    @NotNull
    public Record next();

    public boolean hasNext();

    public void close();

    @Override
    default void remove() {
        Iterator.super.remove();
    }
}

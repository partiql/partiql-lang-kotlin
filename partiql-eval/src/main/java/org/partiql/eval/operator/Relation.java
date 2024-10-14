package org.partiql.eval.operator;

import java.util.Iterator;

/**
 * Relation is the interface for an operator that returns a relation.
 */
public interface Relation extends Operation, AutoCloseable, Iterator<Record> {

    public void open();

    public Record next();

    public boolean hasNext();

    public void close();
}

package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;

import java.util.Iterator;

class PQLToPartiQLIterable implements Iterable<PartiQLValue> {

    @NotNull
    private final Iterable<PQLValue> _values;

    PQLToPartiQLIterable(@NotNull PQLValue value) {
        this._values = value;
    }

    @Override
    public Iterator<PartiQLValue> iterator() {
        Iterator<PQLValue> iter = _values.iterator();
        return new Iterator<PartiQLValue>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public PartiQLValue next() {
                return iter.next().toPartiQLValue();
            }
        };
    }
}

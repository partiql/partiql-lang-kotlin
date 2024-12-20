package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;
import java.util.Iterator;

class PQLToPartiQLIterable implements Iterable<PartiQLValue> {

    @NotNull
    private final Iterable<Datum> _values;

    PQLToPartiQLIterable(@NotNull Datum value) {
        this._values = value;
    }

    @Override
    public Iterator<PartiQLValue> iterator() {
        Iterator<Datum> iter = _values.iterator();
        return new Iterator<PartiQLValue>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public PartiQLValue next() {
                return ValueUtils.newPartiQLValue(iter.next());
            }
        };
    }
}

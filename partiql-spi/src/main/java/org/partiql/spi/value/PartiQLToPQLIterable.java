package org.partiql.spi.value;

import org.partiql.value.CollectionValue;
import org.partiql.value.PartiQLValue;

class PartiQLToPQLIterable implements Iterable<Datum> {

    private final CollectionValue<PartiQLValue> _value;

    PartiQLToPQLIterable(CollectionValue<PartiQLValue> value) {
        _value = value;
    }

    @Override
    public Iterator iterator() {
        return new Iterator(_value.iterator());
    }

    static class Iterator implements java.util.Iterator<Datum> {
        private final java.util.Iterator<PartiQLValue> _value;

        private Iterator(java.util.Iterator<PartiQLValue> value) {
            _value = value;
        }

        @Override
        public boolean hasNext() {
            return _value.hasNext();
        }

        @Override
        public Datum next() {
            PartiQLValue value = _value.next();
            return Datum.of(value);
        }
    }
}

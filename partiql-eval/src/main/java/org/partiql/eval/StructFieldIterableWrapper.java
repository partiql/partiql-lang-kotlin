package org.partiql.eval;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;

class StructFieldIterableWrapper implements Iterable<StructField> {

    private final Iterable<Pair<String, PartiQLValue>> _value;

    StructFieldIterableWrapper(Iterable<Pair<String, PartiQLValue>> value) {
        _value = value;
    }

    @Override
    public Iterator iterator() {
        return new Iterator(_value.iterator());
    }

    static class Iterator implements java.util.Iterator<StructField> {
        private final java.util.Iterator<Pair<String, PartiQLValue>> _value;

        private Iterator(java.util.Iterator<Pair<String, PartiQLValue>> value) {
            _value = value;
        }

        @Override
        public boolean hasNext() {
            return _value.hasNext();
        }

        @Override
        public StructField next() {
            Pair<String, PartiQLValue> value = _value.next();
            return new StructField() {
                @NotNull
                @Override
                public String getName() {
                    return value.getFirst();
                }

                @NotNull
                @Override
                public PQLValue getValue() {
                    return PQLValue.of(value.getSecond());
                }
            };
        }
    }
}

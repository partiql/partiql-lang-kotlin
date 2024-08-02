package org.partiql.eval.value;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;

class PartiQLToPQLStruct implements Iterable<Field> {

    private final Iterable<Pair<String, PartiQLValue>> _value;

    PartiQLToPQLStruct(org.partiql.value.StructValue<PartiQLValue> value) {
        _value = value.getEntries();
    }

    @Override
    public Iterator iterator() {
        return new Iterator(_value.iterator());
    }

    static class Iterator implements java.util.Iterator<Field> {
        private final java.util.Iterator<Pair<String, PartiQLValue>> _value;

        private Iterator(java.util.Iterator<Pair<String, PartiQLValue>> value) {
            _value = value;
        }

        @Override
        public boolean hasNext() {
            return _value.hasNext();
        }

        @Override
        public Field next() {
            Pair<String, PartiQLValue> value = _value.next();
            return new Field() {
                @NotNull
                @Override
                public String getName() {
                    return value.getFirst();
                }

                @NotNull
                @Override
                public Datum getValue() {
                    return Datum.of(value.getSecond());
                }
            };
        }
    }
}

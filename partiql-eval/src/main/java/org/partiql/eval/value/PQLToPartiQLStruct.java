package org.partiql.eval.value;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;

import java.util.Iterator;

class PQLToPartiQLStruct implements Iterable<Pair<String, PartiQLValue>> {

    @NotNull
    PQLValue _value;

    PQLToPartiQLStruct(@NotNull PQLValue value) {
        this._value = value;
    }

    @Override
    public Iterator<Pair<String, PartiQLValue>> iterator() {
        Iterator<StructField> _fields = _value.getFields();

        return new Iterator<Pair<String, PartiQLValue>>() {
            @Override
            public boolean hasNext() {
                return _fields.hasNext();
            }

            @Override
            public Pair<String, PartiQLValue> next() {
                StructField field = _fields.next();
                return new Pair<>(field.getName(), field.getValue().toPartiQLValue());
            }
        };
    }
}

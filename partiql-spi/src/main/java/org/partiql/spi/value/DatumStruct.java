package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.*;

/**
 * This shall always be package-private (internal).
 */
class DatumStruct implements Datum {

    @NotNull
    private final HashMap<String, List<Datum>> _delegate;

    @NotNull
    private final HashMap<String, List<Datum>> _delegateNormalized;

    private final static PType _type = PType.struct();

    DatumStruct(@NotNull Iterable<Field> fields) {
        _delegate = new HashMap<>();
        _delegateNormalized = new HashMap<>();
        for (Field field : fields) {
            String key = field.getName();
            String keyNormalized = field.getName().toLowerCase();
            Datum value = field.getValue();
            addFieldToStruct(_delegate, key, value);
            addFieldToStruct(_delegateNormalized, keyNormalized, value);
        }
    }

    private void addFieldToStruct(Map<String, List<Datum>> struct, String key, Datum value) {
        List<Datum> values = struct.getOrDefault(key, new ArrayList<>());
        values.add(value);
        struct.put(key, values);
    }

    @Override
    @NotNull
    public Iterator<Field> getFields() {
        return _delegate.entrySet().stream().flatMap(
                entry -> entry.getValue().stream().map(
                        value -> Field.of(entry.getKey(), value)
                )
        ).iterator();
    }

    @Override
    public Datum get(@NotNull String name) {
        List<Datum> values = _delegate.get(name);
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public Datum getInsensitive(@NotNull String name) {
        List<Datum> values = _delegateNormalized.get(name.toLowerCase());
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumStruct{" +
                "_type=" + _type +
                ", _value=" + DatumUtils.formatFieldsToString(_delegate) +
                '}';
    }
}

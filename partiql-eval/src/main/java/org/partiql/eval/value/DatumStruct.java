package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This shall always be package-private (internal).
 */
class DatumStruct implements Datum {

    @NotNull
    private final Map<String, List<Datum>> _delegate;

    @NotNull
    private final Map<String, List<Datum>> _delegateNormalized;

    private final static PType _type = PType.typeStruct();

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

    @NotNull
    @Override
    public Datum get(@NotNull String name) {
        try {
            return _delegate.get(name).get(0);
        } catch (IndexOutOfBoundsException ex) {
            throw new NullPointerException("Could not find struct key: " + name);
        }
    }

    @NotNull
    @Override
    public Datum getInsensitive(@NotNull String name) {
        try {
            return _delegateNormalized.get(name).get(0);
        } catch (IndexOutOfBoundsException ex) {
            throw new NullPointerException("Could not find struct key: " + name);
        }
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

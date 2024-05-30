package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This shall always be package-private (internal).
 */
class StructValue implements PQLValue {

    @NotNull
    private final Map<String, List<PQLValue>> _delegate;

    @NotNull
    private final Map<String, List<PQLValue>> _delegateNormalized;

    StructValue(@NotNull Iterable<StructField> fields) {
        _delegate = new HashMap<>();
        _delegateNormalized = new HashMap<>();
        for (StructField field : fields) {
            String key = field.getName();
            String keyNormalized = field.getName().toLowerCase();
            PQLValue value = field.getValue();
            addFieldToStruct(_delegate, key, value);
            addFieldToStruct(_delegateNormalized, keyNormalized, value);
        }
    }

    private void addFieldToStruct(Map<String, List<PQLValue>> struct, String key, PQLValue value) {
        List<PQLValue> values = struct.getOrDefault(key, new ArrayList<>());
        values.add(value);
        struct.put(key, values);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Iterator<StructField> getFields() {
        return _delegate.entrySet().stream().flatMap(
                entry -> entry.getValue().stream().map(
                        value -> StructField.of(entry.getKey(), value)
                )
        ).iterator();
    }

    @NotNull
    @Override
    public PQLValue get(@NotNull String name) {
        try {
            return _delegate.get(name).get(0);
        } catch (IndexOutOfBoundsException ex) {
            throw new NullPointerException("Could not find struct key: " + name);
        }
    }

    @NotNull
    @Override
    public PQLValue getInsensitive(@NotNull String name) {
        try {
            return _delegateNormalized.get(name).get(0);
        } catch (IndexOutOfBoundsException ex) {
            throw new NullPointerException("Could not find struct key: " + name);
        }
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.STRUCT;
    }
}

package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This shall always be package-private (internal).
 */
class StructValue implements PQLValue {

    @NotNull
    private final Iterable<StructField> _value;
    MultiMap map;

    StructValue(@NotNull Iterable<StructField> value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Iterator<StructField> getFields() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public Iterable<StructField> get(@NotNull String name) {
        return map.get(name);
    }

    @NotNull
    @Override
    public Iterable<StructField> getInsensitive(@NotNull String name) {
        return map.get()
        return PQLValue.super.getInsensitive(name);
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.STRUCT;
    }

    private class MultiMap implements Map<String, List<PQLValue>> {

        @NotNull
        private final Map<String, List<PQLValue>> _delegate;

        @NotNull
        private final Map<String, List<PQLValue>> _delegateNormalized;

        private MultiMap(@NotNull Map<String, List<PQLValue>> delegate) {
            _delegate = delegate;
            _delegateNormalized = Collections.emptyMap();
            delegate.forEach((key1, value) -> {
                String key = key1.toLowerCase();
                List<PQLValue> fields = _delegateNormalized.getOrDefault(key, Collections.emptyList());
                fields.addAll(value);
                _delegateNormalized.put(key, fields);
            });
        }

        private MultiMap(@NotNull Iterable<StructField> _value) {
            _delegate = Collections.emptyMap();
            _delegateNormalized = Collections.emptyMap();
            for (StructField field : _value) {
                // Add to delegate
                List<PQLValue> fields = _delegate.getOrDefault(field.getName(), Collections.emptyList());
                fields.add(field.getValue());
                _delegate.put(field.getName(), fields);
                // Add to normalized delegate
                String keyNormalized = field.getName().toLowerCase();
                List<PQLValue> fieldsNormalized = _delegateNormalized.getOrDefault(keyNormalized, Collections.emptyList());
                fieldsNormalized.add(field.getValue());
                _delegateNormalized.put(keyNormalized, fields);
            }
        }

        @Override
        public int size() {
            return _delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return _delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return _delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return _delegate.containsValue(value);
        }

        @Override
        public List<PQLValue> get(Object key) {
            return _delegate.get(key);
        }

        @Override
        public List<StructField> put(String key, List<StructField> value) {
            return null;
        }

        @Override
        public List<StructField> remove(Object key) {
            return null;
        }

        @Override
        public Collection<List<StructField>> values() {
            return null;
        }
    }
}

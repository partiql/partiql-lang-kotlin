package org.partiql.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.SourceLocation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 */
public class SourceLocations implements Map<String, SourceLocation> {

    private final Map<String, SourceLocation> delegate;

    private SourceLocations(Map<String, SourceLocation> delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public Set<Map.Entry<String, SourceLocation>> entrySet() {
        return delegate.entrySet();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @NotNull
    @Override
    public Collection<SourceLocation> values() {
        return delegate.values();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public SourceLocation get(Object key) {
        return delegate.get(key);
    }

    @Nullable
    @Override
    public SourceLocation put(String key, SourceLocation value) {
        // TODO: This isn't allowed (yet?)
        return null;
    }

    @Override
    public SourceLocation remove(Object key) {
        // TODO: This isn't allowed (yet?)
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends SourceLocation> m) {
        // TODO: This isn't allowed (yet?)
    }

    @Override
    public void clear() {
        // TODO: This isn't allowed (yet?)
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * TODO
     */
    public static class Mutable {

        private final Map<String, SourceLocation> delegate = new java.util.HashMap<>();

        /**
         * TODO
         * @param id TODO
         * @param value TODO
         */
        public void set(String id, SourceLocation value) {
            delegate.put(id, value);
        }

        /**
         * TODO
         * @return TODO
         */
        public SourceLocations toMap() {
            return new SourceLocations(delegate);
        }
    }
}


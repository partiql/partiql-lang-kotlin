package org.partiql.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This class maps a set of identifiers to their corresponding source locations.
 * <br>
 * <b>Note!</b>: This class is immutable and does not support {@link Map#put(Object, Object)}, amongst others. Please
 * handle the runtime exceptions indicated by {@link Map}'s Javadocs.
 */
public class SourceLocations implements Map<String, SourceLocation> {

    private final Map<String, SourceLocation> delegate;

    /**
     * Creates an empty instance.
     */
    public SourceLocations() {
        this.delegate = new java.util.HashMap<>();
    }

    /**
     * Creates an instance holding the contents of {@code delegate}. To enforce immutability, these contents are copied
     * to an internal structure.
     * @param delegate the delegate holding the locations.
     */
    public SourceLocations(Map<String, SourceLocation> delegate) {
        this.delegate = new java.util.HashMap<>();
        this.delegate.putAll(delegate);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceLocation remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends SourceLocation> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
}


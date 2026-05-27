package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This shall always be package-private (internal).
 */
class DatumMap implements Datum {

    @NotNull
    private final LinkedHashMap<DatumKey, Datum> _entries;

    @NotNull
    private final PType _type;

    DatumMap(@NotNull PType keyType, @NotNull PType valueType, @NotNull Iterable<Entry> entries) {
        if (keyType.code() == PType.DYNAMIC) {
            throw new IllegalArgumentException("MAP key type must not be DYNAMIC");
        }

        _type = PType.map(keyType, valueType);
        _entries = new LinkedHashMap<>();
        for (Entry entry : entries) {
            _entries.put(new DatumKey(entry.getKey()), entry.getValue());
        }
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public Datum get(@NotNull Datum key) {
        DatumKey wrappedKey = new DatumKey(key);
        Datum value = _entries.get(wrappedKey);
        // null from HashMap means key not found; a NULL value would be stored as Datum.nullValue()
        if (value == null) {
            return Datum.missing();
        }
        return value;
    }

    @Override
    public boolean containsKey(@NotNull Datum key) {
        DatumKey wrappedKey = new DatumKey(key);
        return _entries.containsKey(wrappedKey);
    }

    @NotNull
    @Override
    public Iterator<Entry> getEntries() {
        return _entries.entrySet().stream()
                .map(e -> Entry.of(e.getKey().getDatum(), e.getValue()))
                .iterator();
    }

    /**
     * Returns the number of entries in this MAP.
     */
    public int size() {
        return _entries.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MAP {");
        boolean first = true;
        for (Map.Entry<DatumKey, Datum> entry : _entries.entrySet()) {
            if (!first) sb.append(",");
            sb.append(" ").append(entry.getKey().getDatum()).append(": ").append(entry.getValue());
            first = false;
        }
        if (!_entries.isEmpty()) sb.append(" ");
        sb.append("}");
        return sb.toString();
    }
}

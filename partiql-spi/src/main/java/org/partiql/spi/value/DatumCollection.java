package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType#ARRAY},
 * {@link PType#BAG}
 */
class DatumCollection implements Datum {

    @NotNull
    private final Iterable<Datum> _value;

    @NotNull
    private final PType _type;

    DatumCollection(@NotNull Iterable<Datum> value, @NotNull PType type) {
        _value = value;
        _type = type;
    }

    @NotNull
    @Override
    public Iterator<Datum> iterator() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumCollection{" +
                "_type=" + _type +
                ", _value=" + DatumUtils.formatListToString(_value) +
                '}';
    }
}

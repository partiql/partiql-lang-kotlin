package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumLong implements Datum {

    private final long _value;

    private final static PType _type = PType.bigint();

    DatumLong(long value) {
        _value = value;
    }

    @Override
    public long getLong() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumLong{" +
                "_value=" + _value +
                '}';
    }
}

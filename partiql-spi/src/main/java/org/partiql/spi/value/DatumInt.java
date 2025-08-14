package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumInt implements Datum {

    private final int _value;

    private final static PType _type = PType.integer();

    DatumInt(int value) {
        _value = value;
    }

    @Override
    public int getInt() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumInt{" +
                "_type=" + _type +
                ", _value=" + _value +
                '}';
    }
}

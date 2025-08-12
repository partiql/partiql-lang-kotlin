package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.StringJoiner;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType.Kind#BLOB},
 * {@link PType.Kind#CLOB}
 */
class DatumBytes implements Datum {

    @NotNull
    private final byte[] _value;

    @NotNull
    private final PType _type;

    DatumBytes(@NotNull byte[] value, @NotNull PType type) {
        _value = value;
        _type = type;
    }

    @Override
    @NotNull
    public byte[] getBytes() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (byte b : _value) {
            joiner.add(Byte.toString(b));
        }

        return "DatumBytes{" +
                "_type=" + _type +
                ", _value=" + joiner +
                '}';

    }
}

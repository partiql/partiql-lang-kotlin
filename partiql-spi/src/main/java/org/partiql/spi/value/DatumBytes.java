package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

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
        StringBuilder sb = new StringBuilder();
        for (byte b : _value) {
            sb.append(String.format("%02X", b & 0xFF));
        }

        return "DatumBytes{" +
                "_type=" + _type +
                ", _value=" + sb.toString() +
                '}';
    }
}

package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumShort implements Datum {

    private final short _value;

    private final static PType _type = PType.smallint();

    DatumShort(short value) {
        _value = value;
    }

    @Override
    public short getShort() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

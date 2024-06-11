package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumShort implements Datum {

    private final short _value;

    DatumShort(short value) {
        _value = value;
    }

    @Override
    public short getShort() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.INT16;
    }
}

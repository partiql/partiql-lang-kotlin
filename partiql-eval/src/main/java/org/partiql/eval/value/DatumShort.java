package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumShort implements Datum {

    private final short _value;

    private final static PType _type = PType.typeSmallInt();

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

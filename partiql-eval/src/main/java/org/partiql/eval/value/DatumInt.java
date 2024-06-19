package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumInt implements Datum {

    private final int _value;

    private final static PType _type = PType.typeInt();

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
}

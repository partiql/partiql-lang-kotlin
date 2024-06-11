package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumLong implements Datum {

    private final long _value;

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
        return PType.typeBigInt();
    }
}

package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigInteger;

/**
 * This shall always be package-private (internal).
 */
class DatumBigInteger implements Datum {

    @NotNull
    private final BigInteger _value;

    DatumBigInteger(@NotNull BigInteger value) {
        _value = value;
    }

    @Override
    @NotNull
    public BigInteger getBigInteger() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.INT;
    }
}

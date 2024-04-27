package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigInteger;

/**
 * This shall always be package-private (internal).
 */
class IntValue implements PQLValue {

    @NotNull
    final BigInteger _value;

    IntValue(@NotNull BigInteger value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public BigInteger getIntValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.INT;
    }
}

package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.math.BigInteger;

/**
 * This shall always be package-private (internal).
 */
class CharValue implements PQLValue {

    @NotNull
    final String _value;

    CharValue(@NotNull String value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public String getCharValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.CHAR;
    }
}

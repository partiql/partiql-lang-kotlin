package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class CharValue implements PQLValue {

    @NotNull
    private final String _value;

    CharValue(@NotNull String value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public String getString() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.CHAR;
    }
}

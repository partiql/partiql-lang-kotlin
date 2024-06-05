package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumString implements Datum {

    @NotNull
    private final String _value;

    @NotNull
    private final PartiQLValueType _type;

    DatumString(@NotNull String value, @NotNull PartiQLValueType type) {
        assert(type == PartiQLValueType.STRING || type == PartiQLValueType.SYMBOL);
        _value = value;
        _type = type;
    }

    @Override
    @NotNull
    public String getString() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }
}

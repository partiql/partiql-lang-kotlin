package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumString implements Datum {

    @NotNull
    private final String _value;

    @NotNull
    private final PType _type;

    DatumString(@NotNull String value, @NotNull PType type) {
        assert(type.getKind() == PType.Kind.STRING || type.getKind() == PType.Kind.SYMBOL);
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
    public PType getType() {
        return _type;
    }
}

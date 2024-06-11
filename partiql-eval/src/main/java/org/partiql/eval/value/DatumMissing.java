package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumMissing implements Datum {

    @NotNull
    private final PType _type;

    DatumMissing() {
        _type = PType.typeUnknown();
    }

    DatumMissing(@NotNull PType type) {
        _type = type;
    }

    @Override
    public boolean isMissing() {
        return true;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

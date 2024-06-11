package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumMissing implements Datum {

    @NotNull
    private final PartiQLValueType _type;

    DatumMissing() {
        // TODO: This will likely be UNKNOWN in the future. Potentially something like PostgreSQL's unknown type.
        _type = PartiQLValueType.MISSING;
    }

    DatumMissing(@NotNull PartiQLValueType type) {
        _type = type;
    }

    @Override
    public boolean isMissing() {
        return true;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }
}

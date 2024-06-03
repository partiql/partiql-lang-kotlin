package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class MissingValue implements PQLValue {


    MissingValue() {}

    @Override
    public boolean isNull() {
        return false;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.MISSING;
    }
}

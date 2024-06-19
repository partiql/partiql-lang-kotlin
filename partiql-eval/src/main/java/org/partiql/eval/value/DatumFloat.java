package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumFloat implements Datum {

    private final float _value;

    private final static PType _type = PType.typeReal();

    DatumFloat(float value) {
        _value = value;
    }

    @Override
    public float getFloat() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

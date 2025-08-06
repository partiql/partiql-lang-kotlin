package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumFloat implements Datum {

    private final float _value;

    private final static PType _type = PType.real();

    DatumFloat(float value) {
        if (value == -0e0f) {
            _value = 0e0f;
        } else {
            _value = value;
        }
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

    @Override
    public String toString() {
        return "DatumFloat{" +
                "_type=" + _type +
                ", _value=" + _value +
                '}';
    }
}

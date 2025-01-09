package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumDouble implements Datum {

    private final double _value;
    private final static PType _type = PType.doublePrecision();

    DatumDouble(double value) {
        if (value == -0.0) {
            _value = 0.0;
        } else {
            _value = value;
        }
    }

    @Override
    public double getDouble() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "dp::" + _value;
    }
}

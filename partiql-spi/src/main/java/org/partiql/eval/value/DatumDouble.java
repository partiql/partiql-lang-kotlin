package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumDouble implements Datum {

    private final double _value;
    private final static PType _type = PType.doublePrecision();

    DatumDouble(double value) {
        _value = value;
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
}

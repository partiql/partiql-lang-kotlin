package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumBoolean implements Datum {

    private final boolean _value;

    private final static PType _type = PType.typeBool();

    DatumBoolean(boolean value) {
        _value = value;
    }

    @Override
    public boolean getBoolean() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

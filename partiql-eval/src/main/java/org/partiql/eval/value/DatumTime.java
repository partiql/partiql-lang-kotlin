package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Time;

/**
 * This shall always be package-private (internal).
 */
class DatumTime implements Datum {

    @NotNull
    private final Time _value;

    DatumTime(@NotNull Time value) {
        _value = value;
    }

    @Override
    @NotNull
    public Time getTime() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return PType.typeTimeWithTZ(6); // TODO: Without TZ
    }
}

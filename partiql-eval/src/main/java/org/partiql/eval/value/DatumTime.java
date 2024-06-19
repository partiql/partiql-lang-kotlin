package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Time;

/**
 * This shall always be package-private (internal).
 */
class DatumTime implements Datum {

    @NotNull
    private final Time _value;

    // TODO: Pass precision to constructor.
    // TODO: Create a variant specifically for without TZ
    private final static PType _type = PType.typeTimeWithTZ(6);

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
        return _type;
    }
}

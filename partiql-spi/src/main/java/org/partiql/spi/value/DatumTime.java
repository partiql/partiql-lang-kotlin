package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Time;

/**
 * This shall always be package-private (internal).
 */
class DatumTime implements Datum {

    @NotNull
    private final Time _value;

    // TODO: Create a variant specifically for without TZ
    private final PType _type;

    DatumTime(@NotNull Time value) {
        _value = value;
        _type = PType.timez(6);
    }

    DatumTime(@NotNull Time value, int precision) {
        _value = value;
        _type = PType.timez(precision);
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

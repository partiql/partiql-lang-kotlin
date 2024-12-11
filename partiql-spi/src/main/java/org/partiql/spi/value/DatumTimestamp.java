package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Timestamp;

/**
 * This shall always be package-private (internal).
 */
class DatumTimestamp implements Datum {

    @NotNull
    private final Timestamp _value;

    // TODO: Create a variant specifically for without TZ
    private final PType _type;

    DatumTimestamp(@NotNull Timestamp value) {
        _value = value.toPrecision(6);
        _type = PType.timestampz(6);
    }

    DatumTimestamp(@NotNull Timestamp value, int precision) {
        _value = value.toPrecision(precision);
        _type = PType.timestampz(precision);
    }

    @Override
    @NotNull
    public Timestamp getTimestamp() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

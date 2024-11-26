package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.time.LocalTime;

/**
 * Today we wrap a {@link LocalTime}, in the future we do a 4-byte array to avoid double references.
 */
final class DatumTime implements Datum {

    @NotNull
    private final PType type;

    @NotNull
    private final LocalTime value;

    DatumTime(@NotNull LocalTime value, int precision) {
        this.type = PType.time(precision);
        this.value = value;
    }

    @NotNull
    @Override
    public PType getType() {
        return type;
    }

    @NotNull
    @Override
    public LocalTime getLocalTime() {
        return value;
    }
}

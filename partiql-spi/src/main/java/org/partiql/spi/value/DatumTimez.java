package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.time.LocalTime;
import java.time.OffsetTime;

/**
 * Today we wrap an {@link OffsetTime}, in the future we do a 5-byte array to avoid double references.
 */
final class DatumTimez implements Datum {

    @NotNull
    private final PType type;

    @NotNull
    private final OffsetTime value;

    DatumTimez(@NotNull OffsetTime value, int precision) {
        this.type = PType.timez(precision);
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
        return value.toLocalTime();
    }

    @NotNull
    @Override
    public OffsetTime getOffsetTime() {
        return value;
    }
}

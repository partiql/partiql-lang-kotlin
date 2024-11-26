package org.partiql.spi.value;


import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Today we wrap a {@link LocalDateTime}, in the future we do a 7-byte array to avoid double references.
 */
final class DatumTimestamp implements Datum {

    @NotNull
    private final PType type;

    @NotNull
    private final LocalDateTime value;

    DatumTimestamp(@NotNull LocalDateTime value, int precision) {
        this.type = PType.timestamp(precision);
        this.value = value;
    }

    @NotNull
    @Override
    public PType getType() {
        return type;
    }

    @NotNull
    @Override
    public LocalDate getLocalDate() {
        return value.toLocalDate();
    }

    @NotNull
    @Override
    public LocalTime getLocalTime() {
        return value.toLocalTime();
    }
}

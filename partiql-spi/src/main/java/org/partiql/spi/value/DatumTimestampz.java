package org.partiql.spi.value;


import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

/**
 * Today we wrap an {@link OffsetDateTime}, in the future we do an 8-byte array to avoid double references.
 */
final class DatumTimestampz implements Datum {

    @NotNull
    private final PType type;

    @NotNull
    private final OffsetDateTime value;

    DatumTimestampz(@NotNull OffsetDateTime value, int precision) {
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

    @NotNull
    @Override
    public OffsetTime getOffsetTime() {
        return value.toOffsetTime();
    }

    @NotNull
    @Override
    public LocalDateTime getLocalDateTime() {
        return value.toLocalDateTime();
    }

    @NotNull
    @Override
    public OffsetDateTime getOffsetDateTime() {
        return value;
    }
}
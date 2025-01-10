package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.time.LocalDate;

/**
 * Today we wrap a {@link LocalDate}, in the future we can do a 7-byte array to avoid double references.
 */
final class DatumDate implements Datum {

    @NotNull
    private final PType type;

    @NotNull
    private final LocalDate value;

    DatumDate(@NotNull LocalDate value) {
        this.type = PType.date();
        this.value = value;
    }

    @NotNull
    @Override
    public PType getType() {
        return type;
    }

    @Override
    @NotNull
    public LocalDate getLocalDate() {
        return value;
    }
}

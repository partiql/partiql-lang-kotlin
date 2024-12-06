package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.time.Duration;

/**
 * Today we wrap a {@link Duration}, in the future we can do an 11 byte array.
 */
final class DatumIntervalDT implements Datum {

    @NotNull
    private final Duration value;

    DatumIntervalDT(@NotNull Duration value, int precision) {
        throw new UnsupportedOperationException("INTERVAL_DT not supported");
    }

    @NotNull
    public PType getType() {
        throw new UnsupportedOperationException("INTERVAL_DT not supported");
    }

    @NotNull
    @Override
    public Duration getDuration() {
        return value;
    }
}

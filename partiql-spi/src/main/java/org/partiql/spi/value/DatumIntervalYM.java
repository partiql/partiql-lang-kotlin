package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.time.Period;

/**
 * Today we wrap a {@link Period}, in the future we can do an 11 byte array.
 */
final class DatumIntervalYM implements Datum {

    @NotNull
    private final Period value;

    DatumIntervalYM(@NotNull Period value, int precision) {
        throw new UnsupportedOperationException("INTERVAL_YM not supported");
    }

    @NotNull
    public PType getType() {
        throw new UnsupportedOperationException("INTERVAL_YM not supported");
    }

    @NotNull
    @Override
    public Period getPeriod() {
        return value;
    }
}

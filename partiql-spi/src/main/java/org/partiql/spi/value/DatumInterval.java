package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumInterval implements Datum {

    private final long _value;

    DatumInterval(long value) {
        _value = value;
    }

    @Override
    public long getInterval() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        throw new UnsupportedOperationException("NOT YET IMPLEMENTED");
    }
}

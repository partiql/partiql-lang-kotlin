package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

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
    public PartiQLValueType getType() {
        return PartiQLValueType.INTERVAL;
    }
}

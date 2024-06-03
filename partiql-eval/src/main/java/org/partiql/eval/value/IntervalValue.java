package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class IntervalValue implements PQLValue {

    private final long _value;

    IntervalValue(long value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
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

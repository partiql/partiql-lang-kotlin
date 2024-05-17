package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Time;

import java.util.Objects;

/**
 * This shall always be package-private (internal).
 */
class TimeValue implements PQLValue {

    @NotNull
    private final Time _value;

    TimeValue(@NotNull Time value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Time getTimeValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.TIME;
    }
}

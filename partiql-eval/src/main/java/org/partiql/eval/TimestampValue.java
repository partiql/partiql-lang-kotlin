package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Time;
import org.partiql.value.datetime.Timestamp;

import java.util.Objects;

/**
 * This shall always be package-private (internal).
 */
class TimestampValue implements PQLValue {

    @NotNull
    private final Timestamp _value;

    TimestampValue(@NotNull Timestamp value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Timestamp getTimestamp() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.TIMESTAMP;
    }
}

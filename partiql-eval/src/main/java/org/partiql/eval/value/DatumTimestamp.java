package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Timestamp;

/**
 * This shall always be package-private (internal).
 */
class DatumTimestamp implements Datum {

    @NotNull
    private final Timestamp _value;

    DatumTimestamp(@NotNull Timestamp value) {
        _value = value;
    }

    @Override
    @NotNull
    public Timestamp getTimestamp() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return PType.typeTimestampWithTZ(6); // TODO: Without TZ
    }
}
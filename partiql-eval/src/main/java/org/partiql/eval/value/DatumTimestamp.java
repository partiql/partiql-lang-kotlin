package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
import org.partiql.value.datetime.Timestamp;

/**
 * This shall always be package-private (internal).
 */
class DatumTimestamp implements Datum {

    @NotNull
    private final Timestamp _value;

    // TODO: Pass precision to constructor.
    // TODO: Create a variant specifically for without TZ
    private final static PType _type = PType.typeTimestampWithTZ(6);

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
        return _type;
    }
}

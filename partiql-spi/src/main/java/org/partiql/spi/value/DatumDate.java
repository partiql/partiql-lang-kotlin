package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

/**
 * This shall always be package-private (internal).
 */
class DatumDate implements Datum {

    @NotNull
    private final org.partiql.value.datetime.Date _value;

    private static final PType _type = PType.date();

    DatumDate(@NotNull org.partiql.value.datetime.Date value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public org.partiql.value.datetime.Date getDate() {
        return _value;
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

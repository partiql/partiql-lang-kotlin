package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

/**
 * This shall always be package-private (internal).
 */
class DatumDate implements Datum {

    @NotNull
    private final org.partiql.value.datetime.Date _value;

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
    public PartiQLValueType getType() {
        return PartiQLValueType.DATE;
    }
}

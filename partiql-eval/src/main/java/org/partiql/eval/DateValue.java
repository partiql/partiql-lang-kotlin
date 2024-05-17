package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Date;

/**
 * This shall always be package-private (internal).
 */
class DateValue implements PQLValue {

    @NotNull
    private final Date _value;

    DateValue(@NotNull Date value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Date getDateValue() {
        return _value;
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.DATE;
    }
}

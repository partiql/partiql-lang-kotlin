package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;
import org.partiql.value.datetime.Date;
import org.partiql.value.datetime.Time;

import java.util.Objects;

/**
 * This shall always be package-private (internal).
 */
class DateValue implements PQLValue {

    @NotNull
    final Date _value;

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

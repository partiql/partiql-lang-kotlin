package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 */
class StructValue implements PQLValue {

    @NotNull
    private final Iterable<StructField> _value;

    StructValue(@NotNull Iterable<StructField> value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Iterator<StructField> getStructFields() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.STRUCT;
    }
}

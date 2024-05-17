package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.Collections;
import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 */
class ListValue implements PQLValue {

    @NotNull
    private final Iterable<PQLValue> _value;

    ListValue(@NotNull Iterable<PQLValue> value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Iterator<PQLValue> getListValues() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.LIST;
    }
}

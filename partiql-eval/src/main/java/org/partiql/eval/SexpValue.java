package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.Collections;
import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 */
class SexpValue implements PQLValue {

    @NotNull
    private final Iterable<PQLValue> _value;

    SexpValue(@NotNull Iterable<PQLValue> value) {
        _value = value;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    @NotNull
    public Iterator<PQLValue> getSexpValues() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return PartiQLValueType.SEXP;
    }
}

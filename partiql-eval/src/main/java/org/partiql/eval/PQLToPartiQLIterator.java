package org.partiql.eval;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValue;

import java.util.Iterator;

class PQLToPartiQLIterator implements Iterator<PartiQLValue> {

    @NotNull
    final Iterator<PQLValue> pqlValues;

    PQLToPartiQLIterator(@NotNull Iterator<PQLValue> pqlValues) {
        this.pqlValues = pqlValues;
    }

    @Override
    public boolean hasNext() {
        return pqlValues.hasNext();
    }

    @Override
    public PartiQLValue next() {
        return pqlValues.next().toPartiQLValue();
    }
}

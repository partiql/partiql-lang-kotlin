package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.value.PartiQLValueType;

import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PartiQLValueType#LIST},
 * {@link PartiQLValueType#BAG},
 * {@link PartiQLValueType#SEXP}
 */
class DatumCollection implements Datum {

    @NotNull
    private final Iterable<Datum> _value;

    @NotNull
    private final PartiQLValueType _type;

    DatumCollection(@NotNull Iterable<Datum> value, @NotNull PartiQLValueType type) {
        assert(type == PartiQLValueType.LIST || type == PartiQLValueType.BAG || type == PartiQLValueType.SEXP);
        _value = value;
        _type = type;
    }

    @Override
    public Iterator<Datum> iterator() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PartiQLValueType getType() {
        return _type;
    }
}

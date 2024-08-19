package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType.Kind#ARRAY},
 * {@link PType.Kind#BAG},
 * {@link PType.Kind#SEXP}
 */
class DatumCollection implements Datum {

    @NotNull
    private final Iterable<Datum> _value;

    @NotNull
    private final PType _type;

    DatumCollection(@NotNull Iterable<Datum> value, @NotNull PType type) {
        _value = value;
        _type = type;
    }

    @NotNull
    @Override
    public Iterator<Datum> iterator() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }
}

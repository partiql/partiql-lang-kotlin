package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;
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
    private final PType _type;

    DatumCollection(@NotNull Iterable<Datum> value, @NotNull PType type) {
        assert(type.getKind() == PType.Kind.LIST || type.getKind() == PType.Kind.BAG || type.getKind() == PType.Kind.SEXP);
        _value = value;
        _type = type;
    }

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

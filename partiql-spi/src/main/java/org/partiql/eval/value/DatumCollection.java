package org.partiql.eval.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.util.Iterator;

/**
 * This shall always be package-private (internal).
 * <p></p>
 * This is specifically for:
 * {@link PType.Kind#LIST},
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

    @Override
    public Iterator<Datum> iterator() {
        return _value.iterator();
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_type);
        sb.append("::");
        if (_type.getKind() == PType.Kind.LIST) {
            sb.append("[");
        } else if (_type.getKind() == PType.Kind.BAG) {
            sb.append("<<");
        } else if (_type.getKind() == PType.Kind.SEXP) {
            sb.append("(");
        }
        Iterator<Datum> iter = _value.iterator();
        while (iter.hasNext()) {
            Datum child = iter.next();
            sb.append(child);
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        if (_type.getKind() == PType.Kind.LIST) {
            sb.append("]");
        } else if (_type.getKind() == PType.Kind.BAG) {
            sb.append(">>");
        } else if (_type.getKind() == PType.Kind.SEXP) {
            sb.append(")");
        }
        return sb.toString();
    }
}

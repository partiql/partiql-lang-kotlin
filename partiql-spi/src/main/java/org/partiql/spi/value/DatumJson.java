package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A value of type JSON: one SQL/JSON item. Every part of an item, such as an object's member value or an array's
 * element, is itself a {@link DatumJson}.
 * <p>
 * This shall always be package-private (internal). Build one with the {@code Datum.json*} factories, which check
 * the item's rules.
 */
class DatumJson implements Datum {

    private static final PType TYPE = PType.json();

    private static final DatumJson NULL = new DatumJson(JsonKind.NULL, null, Collections.emptyList(), Collections.emptyList());

    private final int _kind;

    private final Datum _scalar;

    @NotNull
    private final List<Datum> _elements;

    @NotNull
    private final List<Field> _members;

    private DatumJson(int kind, Datum scalar, @NotNull List<Datum> elements, @NotNull List<Field> members) {
        _kind = kind;
        _scalar = scalar;
        _elements = elements;
        _members = members;
    }

    static DatumJson nullItem() {
        return NULL;
    }

    static DatumJson scalar(@NotNull Datum value) {
        if (value.isNull() || value.isMissing()) {
            throw new IllegalArgumentException("An SQL/JSON scalar cannot be NULL or MISSING; use Datum.jsonNull() for SQL/JSON null");
        }
        switch (value.getType().code()) {
            case PType.STRING:
            case PType.INTEGER:
            case PType.BIGINT:
            case PType.DECIMAL:
            case PType.DOUBLE:
            case PType.BOOL:
            case PType.DATE:
            case PType.TIME:
            case PType.TIMEZ:
            case PType.TIMESTAMP:
            case PType.TIMESTAMPZ:
                return new DatumJson(JsonKind.SCALAR, value, Collections.emptyList(), Collections.emptyList());
            default:
                throw new IllegalArgumentException("An SQL/JSON scalar must be a STRING, INTEGER, BIGINT, DECIMAL, DOUBLE, BOOL or datetime, not " + value.getType());
        }
    }

    static DatumJson array(@NotNull Iterable<Datum> elements) {
        List<Datum> list = new ArrayList<>();
        for (Datum element : elements) {
            requireItem(element, "element");
            list.add(element);
        }
        return new DatumJson(JsonKind.ARRAY, null, Collections.unmodifiableList(list), Collections.emptyList());
    }

    static DatumJson object(@NotNull Iterable<Field> members) {
        List<Field> list = new ArrayList<>();
        for (Field member : members) {
            requireItem(member.getValue(), "member value");
            list.add(member);
        }
        return new DatumJson(JsonKind.OBJECT, null, Collections.emptyList(), Collections.unmodifiableList(list));
    }

    /**
     * Each part of an SQL/JSON item is itself an SQL/JSON item: a value of type JSON that is not SQL NULL or MISSING.
     */
    private static void requireItem(@NotNull Datum value, @NotNull String what) {
        if (value.getType().code() != PType.JSON) {
            throw new IllegalArgumentException("An SQL/JSON " + what + " must have type JSON, not " + value.getType());
        }
        if (value.isNull() || value.isMissing()) {
            throw new IllegalArgumentException("An SQL/JSON " + what + " cannot be NULL or MISSING; use Datum.jsonNull() for SQL/JSON null");
        }
    }

    @NotNull
    @Override
    public PType getType() {
        return TYPE;
    }

    @Override
    public int getJsonKind() {
        return _kind;
    }

    @NotNull
    @Override
    public Datum getJsonScalar() {
        if (_kind != JsonKind.SCALAR) {
            throw new InvalidOperationException(TYPE, "getJsonScalar");
        }
        return _scalar;
    }

    @NotNull
    @Override
    public Iterator<Datum> iterator() {
        if (_kind != JsonKind.ARRAY) {
            throw new InvalidOperationException(TYPE, "iterator");
        }
        return _elements.iterator();
    }

    @NotNull
    @Override
    public Iterator<Field> getFields() {
        if (_kind != JsonKind.OBJECT) {
            throw new InvalidOperationException(TYPE, "getFields");
        }
        return _members.iterator();
    }

    @Override
    public String toString() {
        switch (_kind) {
            case JsonKind.NULL:
                return "DatumJson{null}";
            case JsonKind.SCALAR:
                return "DatumJson{" + _scalar + "}";
            case JsonKind.ARRAY:
                return "DatumJson{array, " + _elements.size() + " elements}";
            default:
                return "DatumJson{object, " + _members.size() + " members}";
        }
    }
}

package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.types.PType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A value of type JSON: one SQL/JSON item. Every part of an item, such as an object's member value or an array's
 * element, is itself a {@link DatumJson}.
 * <p>
 * This shall always be package-private (internal). Build one with the {@code Datum.json*} factories, which check
 * the item's rules.
 */
@SuppressWarnings("deprecation")
class DatumJson implements Datum {

    /**
     * The types an SQL/JSON scalar can hold. The list starts narrow on purpose: adding a type later is compatible,
     * and removing one is not.
     */
    private static final Set<Integer> SCALAR_TYPES = new HashSet<>(Arrays.asList(
            PType.STRING,
            PType.INTEGER,
            PType.BIGINT,
            PType.DECIMAL,
            PType.DOUBLE,
            PType.BOOL,
            PType.DATE,
            PType.TIME,
            PType.TIMEZ,
            PType.TIMESTAMP,
            PType.TIMESTAMPZ
    ));

    private static final PType TYPE = PType.json();

    private static final DatumJson NULL = new DatumJson(JsonKind.NULL, null, Collections.emptyList(), Collections.emptyList());

    private final int _kind;

    @Nullable
    private final Datum _scalar;

    @NotNull
    private final List<Datum> _elements;

    @NotNull
    private final List<Field> _members;

    private DatumJson(int kind, @Nullable Datum scalar, @NotNull List<Datum> elements, @NotNull List<Field> members) {
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
        if (!SCALAR_TYPES.contains(value.getType().code())) {
            throw new IllegalArgumentException("An SQL/JSON scalar cannot have type " + value.getType());
        }
        return new DatumJson(JsonKind.SCALAR, value, Collections.emptyList(), Collections.emptyList());
    }

    static DatumJson array(@NotNull Iterable<Datum> elements) {
        List<Datum> list = new ArrayList<>();
        for (Datum element : elements) {
            list.add(requireItem(element, "element"));
        }
        return new DatumJson(JsonKind.ARRAY, null, Collections.unmodifiableList(list), Collections.emptyList());
    }

    static DatumJson object(@NotNull Iterable<Field> members) {
        List<Field> list = new ArrayList<>();
        for (Field member : members) {
            if (member == null || member.getName() == null) {
                throw new IllegalArgumentException("An SQL/JSON object member and its key cannot be null");
            }
            // Read the key and value once, and keep our own copy: Field is a public interface.
            String name = member.getName();
            Datum value = requireItem(member.getValue(), "member value");
            list.add(Field.of(name, value));
        }
        return new DatumJson(JsonKind.OBJECT, null, Collections.emptyList(), Collections.unmodifiableList(list));
    }

    /**
     * Each part of an SQL/JSON item is itself an SQL/JSON item. Only items built by these factories are accepted, so
     * every part has been checked, and an item can't contain itself.
     */
    @NotNull
    private static Datum requireItem(@Nullable Datum value, @NotNull String what) {
        if (value == null) {
            throw new IllegalArgumentException("An SQL/JSON " + what + " cannot be null");
        }
        if (value.isNull() || value.isMissing()) {
            throw new IllegalArgumentException("An SQL/JSON " + what + " cannot be NULL or MISSING; use Datum.jsonNull() for SQL/JSON null");
        }
        if (!(value instanceof DatumJson)) {
            throw new IllegalArgumentException("An SQL/JSON " + what + " must be built with the Datum.json* factories, not " + value.getType());
        }
        return value;
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
        if (_kind != JsonKind.SCALAR || _scalar == null) {
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
        String value;
        switch (_kind) {
            case JsonKind.NULL:
                value = "null";
                break;
            case JsonKind.SCALAR:
                value = String.valueOf(_scalar);
                break;
            case JsonKind.ARRAY:
                value = DatumUtils.formatListToString(_elements);
                break;
            default:
                StringJoiner joiner = new StringJoiner(", ", "{", "}");
                _members.forEach(f -> joiner.add(f.getName() + "=" + f.getValue()));
                value = joiner.toString();
        }
        return "DatumJson{" +
                "_type=" + TYPE +
                ", _value=" + value +
                '}';
    }
}

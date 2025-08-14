package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;
import org.partiql.spi.types.PTypeField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This shall always be package-private (internal).
 */
class DatumRow implements Datum {

    @NotNull
    private final HashMap<String, List<Datum>> _delegate;

    @NotNull
    private final HashMap<String, List<Datum>> _delegateNormalized;

    private final PType _type;

    DatumRow(@NotNull Iterable<Field> fields, @NotNull PType type) {
        _type = type;
        _delegate = new HashMap<>();
        _delegateNormalized = new HashMap<>();
        for (Field field : fields) {
            String key = field.getName();
            String keyNormalized = field.getName().toLowerCase();
            Datum value = field.getValue();
            addFieldToStruct(_delegate, key, value);
            addFieldToStruct(_delegateNormalized, keyNormalized, value);
        }
    }

    DatumRow(@NotNull Iterable<Field> fields) {
        this(fields,  getTypeFromFields(fields));
    }

    private static PType getTypeFromFields(@NotNull Iterable<Field> fields) {
        List<PTypeField> fieldTypes = new ArrayList<>();
        fields.forEach((f) -> {
            PType fType = f.getValue().getType();
            PTypeField typeField = PTypeField.of(f.getName(), fType);
            fieldTypes.add(typeField);
        });
        return PType.row(fieldTypes);
    }

    private void addFieldToStruct(Map<String, List<Datum>> struct, String key, Datum value) {
        List<Datum> values = struct.getOrDefault(key, new ArrayList<>());
        values.add(value);
        struct.put(key, values);
    }

    @Override
    @NotNull
    public Iterator<Field> getFields() {
        return _delegate.entrySet().stream().flatMap(
                entry -> entry.getValue().stream().map(
                        value -> Field.of(entry.getKey(), value)
                )
        ).iterator();
    }

    @Override
    public Datum get(@NotNull String name) {
        List<Datum> values = _delegate.get(name);
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public Datum getInsensitive(@NotNull String name) {
        List<Datum> values = _delegateNormalized.get(name.toLowerCase());
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @NotNull
    @Override
    public PType getType() {
        return _type;
    }

    @Override
    public String toString() {
        return "DatumRow{" +
                "_type=" + _type +
                ", _value=" + DatumUtils.formatFieldsToString(_delegate) +
                '}';
    }
}

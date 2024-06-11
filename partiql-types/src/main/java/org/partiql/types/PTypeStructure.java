package org.partiql.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

class PTypeStructure implements PType {

    @NotNull
    final Kind _kind;

    final Collection<Field> _fields;

    PTypeStructure(@NotNull Kind type, @NotNull Collection<Field> fields) {
        assert(type == Kind.STRUCT || type == Kind.ROW);
        _kind = type;
        _fields = fields;
    }

    PTypeStructure(@NotNull Kind type) {
        assert(type == Kind.STRUCT || type == Kind.ROW);
        _kind = type;
        _fields = null;
    }

    @NotNull
    @Override
    public Kind getKind() {
        return _kind;
    }

    @Override
    public Collection<Field> getFields() {
        return _fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        if (_kind != ((PType) o).getKind()) {
            return false;
        }
        Collection<Field> otherFields = ((PType) o).getFields();
        if (otherFields == null && _fields == null) {
            return true;
        }
        if (otherFields == null || _fields == null) {
            return false;
        }
        int size = _fields.size();
        if (size != otherFields.size()) {
            return false;
        }
        Iterator<Field> thisIter = _fields.iterator();
        Iterator<Field> otherIter = otherFields.iterator();
        for (int i = 0; i < size; i++) {
            Field thisField = thisIter.next();
            Field otherField = otherIter.next();
            if (!thisField.equals(otherField)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (_fields == null) {
            return _kind.name();
        } else {
            Collection<String> fieldStringList = _fields.stream().map((f) -> f.getName() + ": " + f.getType()).collect(Collectors.toList());
            String fieldStrings = String.join(", ", fieldStringList);
            return _kind.name() + "(" + fieldStrings + ")";
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_kind, _fields);
    }
}

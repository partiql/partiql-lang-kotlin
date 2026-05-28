package org.partiql.spi.types;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @deprecated This feature is experimental and is subject to change.
 */
@Deprecated
class PTypeMap extends PType {

    @NotNull
    private final PType _keyType;

    @NotNull
    private final PType _valueType;

    PTypeMap(@NotNull PType keyType, @NotNull PType valueType) {
        super(MAP);
        if (keyType.code() == PType.DYNAMIC) {
            throw new IllegalArgumentException("MAP key type must not be DYNAMIC");
        }
        _keyType = keyType;
        _valueType = valueType;
    }

    @NotNull
    @Override
    public PType getKeyType() {
        return _keyType;
    }

    @NotNull
    @Override
    public PType getValueType() {
        return _valueType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PType)) return false;
        PType other = (PType) o;
        if (other.code() != MAP) return false;
        return other.getKeyType().equals(_keyType) && other.getValueType().equals(_valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code(), _keyType, _valueType);
    }

    @Override
    public String toString() {
        return "MAP(" + _keyType + ", " + _valueType + ")";
    }
}

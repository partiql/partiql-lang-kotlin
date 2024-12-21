package org.partiql.types.shape;


import org.jetbrains.annotations.NotNull;
import org.partiql.types.Field;
import org.partiql.types.PType;
import org.partiql.types.shape.trait.UniqueTrait;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * TODO: Improve API Economic.
 */
public class PShape extends PType {
    private final PType type;

    public PShape(PType type) {
        super(type.code());
        this.type = type;
    }

    public Number maxValue() {
        Number number = null;
        switch (this.code()) {
            case TINYINT:
                number = Byte.MAX_VALUE;
                break;
            case SMALLINT:
                number = Short.MAX_VALUE;
                break;
            case INTEGER:
                number = Integer.MAX_VALUE;
                break;
            case BIGINT:
                number = Long.MAX_VALUE;
                break;
            default:
                throw new UnsupportedOperationException("Retrieving max value not supported for type: " + this.name());
        }
        return number;
    }

    public Number minValue() {
        Number number = null;
        switch (this.code()) {
            case TINYINT:
                number = Byte.MIN_VALUE;
                break;
            case SMALLINT:
                number = Short.MIN_VALUE;
                break;
            case INTEGER:
                number = Integer.MIN_VALUE;
                break;
            case BIGINT:
                number = Long.MIN_VALUE;
                break;
            default:
                throw new UnsupportedOperationException("Retrieving max value not supported for type: " + this.name());
        }
        return number;
    }

    public boolean isNullable() {
        return true;
    }

    public boolean isOptional() {
        return true;
    }

    public Map<String, String> meta() {
        return new HashMap<>();
    }

    public Collection<String> primaryKey() {
        return new ArrayList<>();
    }

    public Collection<String> unique() {
        return new ArrayList<>();
    }

    @Override
    public @NotNull Collection<Field> getFields() throws UnsupportedOperationException {
        return type.getFields();
    }

    @Override
    public int getPrecision() throws UnsupportedOperationException {
        return type.getPrecision();
    }

    @Override
    public int getLength() throws UnsupportedOperationException {
        return type.getLength();
    }

    @Override
    public int getScale() throws UnsupportedOperationException {
        return type.getScale();
    }

    @Override
    public @NotNull PType getTypeParameter() throws UnsupportedOperationException {
        return type.getTypeParameter();
    }

    @Override
    public @NotNull String name() {
        return super.name();
    }

    @Override
    public @NotNull String toString() {
        return type.toString();
    }

    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        PShape that = (PShape) obj;
        return Objects.equals(type, that.type);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return type.hashCode();
    }
}

package org.partiql.types.shape.trait;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.Field;
import org.partiql.types.PType;
import org.partiql.types.shape.PShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * TODO: Improve API Economic.
 * <p>
 * TODO: Equals and HashCode.
 */
public abstract class PTrait extends PShape {
    PShape shape;

    protected PTrait(PShape shape) {
        super(shape);
        this.shape = shape;
    }

    @Override
    public Number maxValue() {
        return shape.maxValue();
    }

    @Override
    public Number minValue() {
        return shape.minValue();
    }

    @Override
    public boolean isNullable() {
        return shape.isNullable();
    }

    @Override
    public boolean isOptional() {
        return shape.isOptional();
    }

    @Override
    public Map<String, String> meta() {
        return shape.meta();
    }

    @Override
    public Collection<String> primaryKey() {
        return shape.primaryKey();
    }

    public Collection<String> unique() {
        return shape.unique();
    }

    @Override
    public @NotNull Collection<Field> getFields() throws UnsupportedOperationException {
        return shape.getFields();
    }

    @Override
    public int getPrecision() throws UnsupportedOperationException {
        return shape.getPrecision();
    }

    @Override
    public int getLength() throws UnsupportedOperationException {
        return shape.getLength();
    }

    @Override
    public int getScale() throws UnsupportedOperationException {
        return shape.getScale();
    }

    @Override
    public @NotNull PType getTypeParameter() throws UnsupportedOperationException {
        return shape.getTypeParameter();
    }

    @Override
    public @NotNull String name() {
        return shape.name();
    }

    @Override
    public @NotNull String toString() {
        return shape.toString();
    }

    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        PTrait that = (PTrait) obj;
        return Objects.equals(shape, that.shape);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return shape.hashCode();
    }
}

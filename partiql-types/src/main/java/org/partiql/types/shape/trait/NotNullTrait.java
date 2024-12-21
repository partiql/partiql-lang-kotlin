package org.partiql.types.shape.trait;

import org.partiql.types.shape.PShape;

import java.util.Objects;

/**
 * TODO: Improve API Economic.
 * <p>
 * TODO: Equals and HashCode.
 */
public class NotNullTrait extends PTrait {
    public NotNullTrait(PShape shape) {
        super(shape);
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        NotNullTrait that = (NotNullTrait) obj;
        return Objects.equals(shape, that.shape);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return shape.hashCode();
    }
}

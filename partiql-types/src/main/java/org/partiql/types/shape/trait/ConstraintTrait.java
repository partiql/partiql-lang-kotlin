package org.partiql.types.shape.trait;

import org.partiql.types.shape.PShape;

import java.util.Objects;

/**
 * TODO: Improve API Economic.
 * <p>
 * TODO: Equals and HashCode.
 */
public class ConstraintTrait extends PTrait {
    private final String expression;

    public ConstraintTrait(PShape shape, String expression) {
        super(shape);
        this.expression = expression;
    }

    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        ConstraintTrait that = (ConstraintTrait) obj;
        if (!Objects.equals(expression, that.expression)) return false;
        return Objects.equals(shape, that.shape);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return shape.hashCode();
    }
}

package org.partiql.types.shape.trait;

import org.partiql.types.shape.PShape;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * TODO: Improve API Economic.
 * <p>
 * TODO: Equals and HashCode.
 */
public class PrimaryKeyTrait extends PTrait {
    private final List<String> identifier;

    public PrimaryKeyTrait(PShape shape, List<String> identifier) {
        super(shape);
        this.identifier = identifier;
    }

    @Override
    public Collection<String> primaryKey() {
        return identifier;
    }

    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        PrimaryKeyTrait that = (PrimaryKeyTrait) obj;
        if (!identifier.containsAll(that.identifier)) return false;
        if (!that.identifier.containsAll(identifier)) return false;
        return Objects.equals(shape, that.shape);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return shape.hashCode();
    }
}

package org.partiql.types.shape.trait;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.shape.PShape;

import java.util.Map;
import java.util.Objects;

/**
 * TODO: Improve API Economic.
 * <p>
 * TODO: Equals and HashCode.
 */
public class MetadataTrait extends PTrait {
    @NotNull
    private final String name;
    @NotNull
    private final String value;

    public MetadataTrait(PShape shape, @NotNull String name, @NotNull String value) {
        super(shape);
        this.name = name;
        this.value = value;
    }

    @Override
    public Map<String, String> meta() {
        Map<String, String> map = super.meta();
        map.put(name, value);
        return map;
    }


    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        MetadataTrait that = (MetadataTrait) obj;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(value, that.value)) return false;
        return Objects.equals(shape, that.shape);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return shape.hashCode();
    }
}

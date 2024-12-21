package org.partiql.types.shape.trait;

import org.partiql.types.shape.PShape;

import java.util.Objects;

/**
 * TODO: Improve API Economic.
 * <p>
 * TODO: Equals and HashCode.
 */
public class RangeTrait extends PTrait {
    private final Number minValue;
    private final Number maxValue;

    public RangeTrait(PShape shape, Number minValue, Number maxValue) {
        super(shape);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public Number minValue() {
        if (minValue == null) {
            return super.minValue();
        }

        return Math.max(shape.minValue().doubleValue(), minValue.doubleValue());
    }

    @Override
    public Number maxValue() {
        if (maxValue == null) {
            return super.maxValue();
        }
        return Math.min(shape.maxValue().doubleValue(), maxValue.doubleValue());
    }

    @Override
    // TODO: Revisit Equals and hasCode function
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        RangeTrait that = (RangeTrait) obj;
        if (!Objects.equals(maxValue, that.maxValue)) return false;
        if (!Objects.equals(minValue, that.minValue)) return false;
        return Objects.equals(shape, that.shape);
    }

    // TODO: Revisit Equals and hasCode function
    @Override
    public int hashCode() {
        return shape.hashCode();
    }
}

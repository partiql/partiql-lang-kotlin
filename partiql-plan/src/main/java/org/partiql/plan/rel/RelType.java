package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PTypeField;

/**
 * Analogous to a ROW type, consider cardinality estimates or other hint mechanisms.
 */
public final class RelType {

    public static final int ORDERED = 0x01;

    private final PTypeField[] fields;
    private final boolean ordered;

    private RelType(PTypeField[] fields, boolean ordered) {
        this.fields = fields;
        this.ordered = ordered;
    }

    @NotNull
    public static RelType of(PTypeField... fields) {
        return of(fields, 0);
    }

    @NotNull
    public static RelType of(PTypeField[] fields, int properties) {
        boolean ordered = (properties & ORDERED) != 0;
        return new RelType(fields, ordered);
    }

    /**
     * The degree (number of fields) in this ROW type.
     * @return the degree of this ROW type
     */
    public int getDegree() {
        return fields.length;
    }

    @NotNull
    public PTypeField[] getFields() {
        return fields;
    }

    @NotNull
    public PTypeField getField(int index) {
        if (index < 0 || index >= fields.length) {
            throw new IllegalArgumentException("field index out of bounds: " + index);
        }
        return fields[index]; // bounds check?
    }

    @NotNull
    public PTypeField getField(String name) {
        for (PTypeField field : fields) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new IllegalArgumentException("field name not found: " + name);
    }

    /**
     * @return true if the rel produces an ordered stream of rows.
     */
    public boolean isOrdered() {
        return ordered;
    }
}

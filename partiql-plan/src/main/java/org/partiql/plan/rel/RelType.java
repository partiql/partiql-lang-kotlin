package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.Field;

/**
 * Analogous to a ROW type, consider cardinality estimates or other hint mechanisms.
 */
public final class RelType {

    public static final int ORDERED = 0x01;

    private final Field[] fields;
    private final boolean ordered;

    private RelType(Field[] fields, boolean ordered) {
        this.fields = fields;
        this.ordered = ordered;
    }

    @NotNull
    public static RelType of(Field... fields) {
        return of(fields, 0);
    }

    @NotNull
    public static RelType of(Field[] fields, int properties) {
        boolean ordered = (properties & ORDERED) != 0;
        return new RelType(fields, ordered);
    }

    public int getDegree() {
        return fields.length;
    }

    @NotNull
    public Field[] getFields() {
        return fields;
    }

    @NotNull
    public Field getField(int index) {
        if (index < 0 || index >= fields.length) {
            throw new IllegalArgumentException("field index out of bounds: " + index);
        }
        return fields[index]; // bounds check?
    }

    @NotNull
    public Field getField(String name) {
        for (Field field : fields) {
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

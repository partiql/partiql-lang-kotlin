package org.partiql.eval;

import org.partiql.spi.value.Datum;

import java.util.Arrays;
import java.util.Objects;

/**
 * A record is an ordered collection of values e.g. tuple.
 */
public class Row {

    /**
     * TODO internalize values.
     */
    public final Datum[] values;

    /**
     * TODO keep ??
     *
     * @param values the values
     * @return the record
     */
    public static Row of(Datum... values) {
        return new Row(values);
    }

    /**
     * Create an empty record.
     */
    public Row() {
        this.values = new Datum[]{};
    }

    /**
     * Create a record with the given values.
     *
     * @param values the values
     */
    public Row(Datum[] values) {
        this.values = values;
    }

    /**
     * Concatenates this record with another record.
     *
     * @param other the other record
     * @return the concatenated record
     */
    public Row concat(Row other) {
        Datum[] result = Arrays.copyOf(this.values, this.values.length + other.values.length);
        System.arraycopy(other.values, 0, result, this.values.length, other.values.length);
        return new Row(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Row)) return false;
        Row row = (Row) o;
        return Objects.deepEquals(values, row.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        for (int i = 0; i < values.length; i++) {
            sb.append(i).append(": ").append(values[i]);
            if (i < values.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(">");
        return sb.toString();
    }
}

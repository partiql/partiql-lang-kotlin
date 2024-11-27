package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.Enum;

/**
 * Represents a collation, which is a resolved sort specification.
 */
public interface Collation {

    /**
     * API WARNING – THIS WILL BE REPLACED WITH AN `int` IN 1.0.
     * <br>
     * TODO replace with an `int` in 1.0
     * TODO <a href="https://github.com/partiql/partiql-lang-kotlin/issues/1664">...</a>
     *
     * @return the column to sort by
     */
    public Rex getColumn();

    /**
     * @return ASC, DESC, or OTHER
     */
    public Order getOrder();

    /**
     * @return NULL ordering
     */
    public Nulls getNulls();

    /**
     * Collation value ordering.
     */
    public final class Order extends Enum {

        private Order(int code) {
            super(code);
        }

        public static final int UNKNOWN = 0;
        public static final int ASC = 1;
        public static final int DESC = 2;

        @NotNull
        public static Order ASC() {
            return new Order(ASC);
        }

        @NotNull
        public static Order DESC() {
            return new Order(DESC);
        }

        @Override
        public String toString() {
            int code = code();
            switch (code) {
                case ASC:
                    return "ASC";
                case DESC:
                    return "DESC";
                default:
                    return String.valueOf(code);
            }
        }
    }

    /**
     * Collation null ordering.
     */
    public final class Nulls extends Enum {

        private Nulls(int code) {
            super(code);
        }

        public static final int UNKNOWN = 0;
        public static final int FIRST = 1;
        public static final int LAST = 2;

        @NotNull
        public static Order FIRST() {
            return new Order(FIRST);
        }

        @NotNull
        public static Order LAST() {
            return new Order(LAST);
        }

        @Override
        public String toString() {
            int code = code();
            switch (code) {
                case FIRST:
                    return "FIRST";
                case LAST:
                    return "LAST";
                default:
                    return String.valueOf(code);
            }
        }
    }
}

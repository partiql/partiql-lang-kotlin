package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.Enum;
import org.partiql.spi.UnsupportedCodeException;

/**
 * Represents a collation, which is a resolved sort specification.
 */
public interface Collation {

    /**
     * API WARNING – THIS WILL BE REPLACED WITH AN `int` IN 1.0.
     * <br>
     * TODO <a href="https://github.com/partiql/partiql-lang-kotlin/issues/1664">replace with an `int` in 1.0</a>
     *
     * @return the column to sort by
     */
    Rex getColumn();

    /**
     * @return ASC, DESC, or OTHER
     */
    Order getOrder();

    /**
     * @return NULL ordering
     */
    Nulls getNulls();

    /**
     * Collation value ordering.
     */
    final class Order extends Enum {

        private Order(int code) {
            super(code);
        }

        @NotNull
        @Override
        public String name() throws UnsupportedCodeException {
            int code = code();
            switch (code) {
                case ASC:
                    return "ASC";
                case DESC:
                    return "DESC";
                default:
                    throw new UnsupportedCodeException(code);
            }
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
    }

    /**
     * Collation null ordering.
     */
    final class Nulls extends Enum {

        private Nulls(int code) {
            super(code);
        }

        public static final int UNKNOWN = 0;
        public static final int FIRST = 1;
        public static final int LAST = 2;

        @NotNull
        public static Nulls FIRST() {
            return new Nulls(FIRST);
        }

        @NotNull
        public static Nulls LAST() {
            return new Nulls(LAST);
        }

        @NotNull
        @Override
        public String name() throws UnsupportedCodeException {
            int code = code();
            switch (code) {
                case FIRST:
                    return "FIRST";
                case LAST:
                    return "LAST";
                default:
                    throw new UnsupportedCodeException(code);
            }
        }
    }
}

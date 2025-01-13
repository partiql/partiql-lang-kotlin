package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.types.PType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.partiql.spi.types.PType.ARRAY;
import static org.partiql.spi.types.PType.BAG;
import static org.partiql.spi.types.PType.BIGINT;
import static org.partiql.spi.types.PType.BLOB;
import static org.partiql.spi.types.PType.BOOL;
import static org.partiql.spi.types.PType.CHAR;
import static org.partiql.spi.types.PType.CLOB;
import static org.partiql.spi.types.PType.DATE;
import static org.partiql.spi.types.PType.DECIMAL;
import static org.partiql.spi.types.PType.DOUBLE;
import static org.partiql.spi.types.PType.DYNAMIC;
import static org.partiql.spi.types.PType.INTEGER;
import static org.partiql.spi.types.PType.NUMERIC;
import static org.partiql.spi.types.PType.REAL;
import static org.partiql.spi.types.PType.SMALLINT;
import static org.partiql.spi.types.PType.STRING;
import static org.partiql.spi.types.PType.STRUCT;
import static org.partiql.spi.types.PType.TIME;
import static org.partiql.spi.types.PType.TIMESTAMP;
import static org.partiql.spi.types.PType.TIMESTAMPZ;
import static org.partiql.spi.types.PType.TIMEZ;
import static org.partiql.spi.types.PType.TINYINT;
import static org.partiql.spi.types.PType.UNKNOWN;
import static org.partiql.spi.types.PType.VARCHAR;
import static org.partiql.spi.types.PType.ROW;

/**
 * This class allows for the comparison between two {@link Datum}s. This is internally implemented by constructing
 * a comparison table, where each cell contains a reference to a {@link DatumComparison} to compute the comparison.
 * The table's rows and columns are indexed by the {@link PType#code()}. The first dimension matches the
 * left-hand-side's type of {@link #compare(Datum lhs, Datum rhs)}. The second dimension matches the right-hand-side's
 * type of {@link #compare(Datum lhs, Datum rhs)}. As such, this implementation allows for O(1) comparison of scalars.
 */
abstract class DatumComparator implements Comparator<Datum> {

    private static final int EQUAL = 0;

    private static final int LESS = -1;

    private static final int GREATER = 1;

    @NotNull
    private static final int[] TYPE_KINDS = PType.codes();

    private static final int TYPE_KINDS_LENGTH = TYPE_KINDS.length;

    /**
     * This array defines the precedence of type families when comparing values of different types. The lower the index,
     * the higher the precedence. Please see
     * <a href="https://partiql.org/partiql-lang/#sec:order-by-less-than">PartiQL Specification Section 12.2</a> for
     * more information.
     * <p>
     * This is only used for aiding in the initialization of the {@link #COMPARISON_TABLE}.
     * </p>
     */
    @NotNull
    private static final Map<Integer, Integer> TYPE_PRECEDENCE = initializeTypePrecedence();

    /**
     * <p>
     * Holds a two-dimensional array of comparison functions. The first dimension represents the LHS of a comparison. The
     * second dimension represents the RHS of a comparison.
     * </p>
     * <p>
     * To check the comparison of two {@link Datum}'s, use the following syntax:
     * <code>
     *     COMPARISON_TABLE[lhs_kind][rhs_kind].apply(lhs, rhs)
     * </code>
     * Note that each {@link Datum} must not be null or missing.
     * </p>
     */
    private static final DatumComparison[][] COMPARISON_TABLE = initializeComparators();

    /**
     * @return {@link DatumComparator#LESS} or {@link DatumComparator#GREATER} depending on the NULLS FIRST or NULLS
     * LAST configuration.
     */
    abstract int lhsUnknown();

    /**
     * @return {@link DatumComparator#LESS} or {@link DatumComparator#GREATER} depending on the NULLS FIRST or NULLS
     * LAST configuration.
     */
    abstract int rhsUnknown();

    @Override
    public int compare(Datum lhs, Datum rhs) {
        // Check if  NULL/MISSING
        Integer result = checkUnknown(lhs, rhs);
        if (result != null) {
            return result;
        }

        // Check for VARIANT & if NULL/MISSING
        boolean lhsIsVariant = lhs.getType().code() == PType.VARIANT;
        boolean rhsIsVariant = rhs.getType().code() == PType.VARIANT;
        Datum lhsActual = lhsIsVariant ? lhs.lower() : lhs;
        Datum rhsActual = rhsIsVariant ? rhs.lower() : rhs;
        if (lhsIsVariant || rhsIsVariant) {
            result = checkUnknown(lhsActual, rhsActual);
            if (result != null) {
                return result;
            }
        }

        // Invoke the Comparison Table
        int lhsKind = lhsActual.getType().code();
        int rhsKind = rhsActual.getType().code();
        return COMPARISON_TABLE[lhsKind][rhsKind].apply(lhsActual, rhsActual, this);
    }

    /**
     * @param lhs the left side
     * @param rhs the right side
     * @return null if both are NOT unknown (AKA, they are both concrete); the result if one or more is unknown.
     */
    private Integer checkUnknown(Datum lhs, Datum rhs) {
        boolean lhsIsUnknown = lhs.isNull() || lhs.isMissing();
        boolean rhsIsUnknown = rhs.isNull() || rhs.isMissing();
        if (lhsIsUnknown && rhsIsUnknown) {
            return EQUAL;
        }
        if (lhsIsUnknown) {
            return lhsUnknown();
        }
        if (rhsIsUnknown) {
            return rhsUnknown();
        }
        return null;
    }

    /**
     * Orders NULL/MISSING values first.
     * <p></p>
     * Use this comparator with {@link java.util.Collections#sort(java.util.List, java.util.Comparator)} to sort a list
     * with NULL/MISSING values first.
     */
    static class NullsFirst extends DatumComparator {

        @Override
        int lhsUnknown() {
            return LESS;
        }

        @Override
        int rhsUnknown() {
            return GREATER;
        }
    }

    /**
     * Orders NULL/MISSING values last.
     * <p></p>
     * Use this comparator with {@link java.util.Collections#sort(java.util.List, java.util.Comparator)} to sort a list
     * with NULL/MISSING values last.
     */
    static class NullsLast extends DatumComparator {

        @Override
        int lhsUnknown() {
            return GREATER;
        }

        @Override
        int rhsUnknown() {
            return LESS;
        }
    }

    /**
     * @return the precedence of the types for the PartiQL comparator.
     * @see #TYPE_PRECEDENCE
     */
    @NotNull
    private static Map<Integer, Integer> initializeTypePrecedence() {
        Map<Integer, Integer> precedence = new HashMap<>();
        // Boolean Type
        precedence.put(BOOL, 0);
        // Number Types
        precedence.put(TINYINT, 1);
        precedence.put(SMALLINT, 1);
        precedence.put(INTEGER, 1);
        precedence.put(BIGINT, 1);
        precedence.put(NUMERIC, 1);
        precedence.put(DECIMAL, 1);
        precedence.put(REAL, 1);
        precedence.put(DOUBLE, 1);
        // Date Type
        precedence.put(DATE, 2);
        // Time Type
        precedence.put(TIMEZ, 3);
        precedence.put(TIME, 3);
        // Timestamp Types
        precedence.put(TIMESTAMPZ, 4);
        precedence.put(TIMESTAMP, 4);
        // Text Types
        precedence.put(CHAR, 5);
        precedence.put(PType.VARCHAR, 5);
        precedence.put(STRING, 5);
        // LOB Types
        precedence.put(CLOB, 6);
        precedence.put(BLOB, 6);
        // Array Type
        precedence.put(ARRAY, 7);
        // Tuple Type
        precedence.put(PType.ROW, 9);
        precedence.put(STRUCT, 9);
        // Bag Type
        precedence.put(BAG, 10);
        // OTHER
        precedence.put(DYNAMIC, 100);
        precedence.put(UNKNOWN, 100);
        precedence.put(PType.VARIANT, 100);
        return precedence;
    }

    /**
     * This essentially operates in two passes.
     * <ol>
     * <li>Initialize the comparisons for the cartesian product of all type kinds based solely on
     * the {@link #TYPE_PRECEDENCE}.</li>
     * <li>Rewrite the comparisons where the values themselves will need to be compared. For example, all PartiQL numbers
     * need to have their JVM primitives extracted before making comparison judgements.</li>
     * </ol>
     * @return the 2D comparison table
     * @see #initializeComparatorArray(int)
     * @see #fillIntComparator(DatumComparison[])
     */
    private static DatumComparison[][] initializeComparators() {
        // Initialize Table
        DatumComparison[][] table = new DatumComparison[TYPE_KINDS_LENGTH][TYPE_KINDS_LENGTH];
        for (int i = 0; i < TYPE_KINDS_LENGTH; i++) {
            @SuppressWarnings("ConstantConditions")
            int kind = TYPE_KINDS[i];
            DatumComparison[] row = initializeComparatorArray(kind);
            table[i] = row;
            switch (kind) {
                case DYNAMIC:
                case UNKNOWN:
                    break;
                case BOOL:
                    fillBooleanComparator(row);
                    break;
                case TINYINT:
                    fillTinyIntComparator(row);
                    break;
                case SMALLINT:
                    fillSmallIntComparator(row);
                    break;
                case INTEGER:
                    fillIntComparator(row);
                    break;
                case BIGINT:
                    fillBigIntComparator(row);
                    break;
                case NUMERIC:
                case DECIMAL:
                    fillDecimalComparator(row);
                    break;
                case REAL:
                    fillRealComparator(row);
                    break;
                case DOUBLE:
                    fillDoubleComparator(row);
                    break;
                case CHAR:
                case VARCHAR:
                case STRING:
                    fillStringComparator(row);
                    break;
                case BLOB:
                case CLOB:
                    fillLobComparator(row);
                    break;
                case DATE:
                    fillDateComparator(row);
                    break;
                case TIMEZ:
                case TIME:
                    fillTimeComparator(row);
                    break;
                case TIMESTAMPZ:
                case TIMESTAMP:
                    fillTimestampComparator(row);
                    break;
                case BAG:
                    fillBagComparator(row);
                    break;
                case ARRAY:
                    fillListComparator(row);
                    break;
                case ROW:
                case STRUCT:
                    fillStructComparator(row);
                    break;
                default:
                    break;
            }
        }
        // Register
        return table;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillTinyIntComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> Byte.compare(self.getByte(), tinyInt.getByte());
        comps[SMALLINT] = (self, smallInt, comp) -> Short.compare(self.getByte(), smallInt.getShort());
        comps[INTEGER] = (self, intNum, comp) -> Integer.compare(self.getByte(), intNum.getInt());
        comps[BIGINT] = (self, bigInt, comp) -> Long.compare(self.getByte(), bigInt.getLong());
        comps[NUMERIC] = (self, intArbitrary, comp) -> BigDecimal.valueOf(self.getByte()).compareTo(intArbitrary.getBigDecimal());
        comps[REAL] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getByte(), real.getFloat()));
        comps[DOUBLE] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getByte(), doublePrecision.getDouble()));
        comps[DECIMAL] = (self, decimal, comp) -> BigDecimal.valueOf(self.getByte()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillSmallIntComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> Short.compare(self.getShort(), tinyInt.getByte());
        comps[SMALLINT] = (self, smallInt, comp) -> Short.compare(self.getShort(), smallInt.getShort());
        comps[INTEGER] = (self, intNum, comp) -> Integer.compare(self.getShort(), intNum.getInt());
        comps[BIGINT] = (self, bigInt, comp) -> Long.compare(self.getShort(), bigInt.getLong());
        comps[NUMERIC] = (self, intArbitrary, comp) -> BigDecimal.valueOf(self.getShort()).compareTo(intArbitrary.getBigDecimal());
        comps[REAL] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getShort(), real.getFloat()));
        comps[DOUBLE] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getShort(), doublePrecision.getDouble()));
        comps[DECIMAL] = (self, decimal, comp) -> BigDecimal.valueOf(self.getShort()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillIntComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> Integer.compare(self.getInt(), tinyInt.getByte());
        comps[SMALLINT] = (self, smallInt, comp) -> Integer.compare(self.getInt(), smallInt.getShort());
        comps[INTEGER] = (self, intNum, comp) -> Integer.compare(self.getInt(), intNum.getInt());
        comps[BIGINT] = (self, bigInt, comp) -> Long.compare(self.getInt(), bigInt.getLong());
        comps[NUMERIC] = (self, intArbitrary, comp) -> BigDecimal.valueOf(self.getInt()).compareTo(intArbitrary.getBigDecimal());
        comps[REAL] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getInt(), real.getFloat()));
        comps[DOUBLE] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getInt(), doublePrecision.getDouble()));
        comps[DECIMAL] = (self, decimal, comp) -> BigDecimal.valueOf(self.getInt()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillBigIntComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> Long.compare(self.getLong(), tinyInt.getByte());
        comps[SMALLINT] = (self, smallInt, comp) -> Long.compare(self.getLong(), smallInt.getShort());
        comps[INTEGER] = (self, intNum, comp) -> Long.compare(self.getLong(), intNum.getInt());
        comps[BIGINT] = (self, bigInt, comp) -> Long.compare(self.getLong(), bigInt.getLong());
        comps[NUMERIC] = (self, intArbitrary, comp) -> BigDecimal.valueOf(self.getLong()).compareTo(intArbitrary.getBigDecimal());
        comps[REAL] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getLong(), real.getFloat()));
        comps[DOUBLE] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getLong(), doublePrecision.getDouble()));
        comps[DECIMAL] = (self, decimal, comp) -> BigDecimal.valueOf(self.getLong()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillRealComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), tinyInt.getByte()));
        comps[SMALLINT] = (self, smallInt, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), smallInt.getShort()));
        comps[INTEGER] = (self, intNum, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), intNum.getInt()));
        comps[BIGINT] = (self, bigInt, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), bigInt.getLong()));
        comps[NUMERIC] = (self, intArbitrary, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), intArbitrary.getBigDecimal().floatValue()));
        comps[REAL] = (self, real, comp) -> compareDoubles(self.getFloat(), real.getFloat(), () -> Float.compare(self.getFloat(), real.getFloat()));
        comps[DOUBLE] = (self, doublePrecision, comp) -> {
            float selfFlt = self.getFloat();
            double otherDbl = doublePrecision.getDouble();
            return compareDoubles(selfFlt, otherDbl, () -> Double.compare(selfFlt, otherDbl));
        };
        comps[DECIMAL] = (self, decimal, comp) -> compareDoubleLhs(self.getFloat(), () -> BigDecimal.valueOf(self.getFloat()).compareTo(decimal.getBigDecimal()));
        return comps;
    }

    /**
     * Handles NaN, -Inf, and +Inf when the {@code lhs} is a floating-point number.
     * @param lhs the floating point lhs argument
     * @param rhs the floating-point rhs argument
     * @return when the {@code lhs} and/or {@code rhs} is Nan, -Inf, or +Inf, the comparison result; else,
     * the {@code comparison} result.
     */
    private static int compareDoubles(double lhs, double rhs, Supplier<Integer> func) {
        // NaN check
        boolean lhsIsNan = Double.isNaN(lhs);
        boolean rhsIsNan = Double.isNaN(rhs);
        if (lhsIsNan && rhsIsNan) {
            return EQUAL;
        }
        if (lhsIsNan) {
            return LESS;
        }
        if (rhsIsNan) {
            return GREATER;
        }
        // Negative infinity check
        boolean lhsIsNegativeInf = Double.isInfinite(lhs) && lhs < 0;
        boolean rhsIsNegativeInf = Double.isInfinite(rhs) && rhs < 0;
        if (lhsIsNegativeInf && rhsIsNegativeInf) {
            return EQUAL;
        }
        if (lhsIsNegativeInf) {
            return LESS;
        }
        if (rhsIsNegativeInf) {
            return GREATER;
        }
        // Positive infinity check
        boolean lhsIsPositiveInf = Double.isInfinite(lhs) && lhs > 0;
        boolean rhsIsPositiveInf = Double.isInfinite(rhs) && rhs > 0;
        if (lhsIsPositiveInf && rhsIsPositiveInf) {
            return EQUAL;
        }
        if (lhsIsPositiveInf) {
            return GREATER;
        }
        if (rhsIsPositiveInf) {
            return LESS;
        }
        // Zero check
        boolean lhsIsZero = lhs == 0.0;
        boolean rhsIsZero = rhs == 0.0;
        if (lhsIsZero && rhsIsZero) {
            return EQUAL;
        }
        // Default extraction and comparison
        return func.get();
    }

    /**
     * Handles NaN, -Inf, and +Inf when the {@code lhs} is a floating-point number.
     * @param lhs the double to be checked
     * @return when the {@code lhs} is Nan, -Inf, or +Inf, the comparison result; else, the {@code comparison} result.
     */
    private static int compareDoubleLhs(double lhs, Supplier<Integer> func) {
        if (Double.isNaN(lhs)) {
            return LESS;
        }
        if (Double.isInfinite(lhs)) {
            if (lhs > 0) {
                return GREATER;
            }
            if (lhs < 0) {
                return LESS;
            }
        }
        return func.get();
    }

    /**
     * Handles NaN, -Inf, and +Inf when the {@code rhs} is a floating-point number.
     * @param rhs the double to be checked
     * @return when the {@code rhs} is Nan, -Inf, or +Inf, the comparison result; else, the {@code comparison} result.
     */
    private static int compareDoubleRhs(double rhs, Supplier<Integer> comparison) {
        if (Double.isNaN(rhs)) {
            return GREATER;
        }
        if (Double.isInfinite(rhs)) {
            if (rhs > 0) {
                return LESS;
            }
            if (rhs < 0) {
                return GREATER;
            }
        }
        return comparison.get();
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillDoubleComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), tinyInt.getByte()));
        comps[SMALLINT] = (self, smallInt, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), smallInt.getShort()));
        comps[INTEGER] = (self, intNum, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), intNum.getInt()));
        comps[BIGINT] = (self, bigInt, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), bigInt.getLong()));
        comps[NUMERIC] = (self, intArbitrary, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), intArbitrary.getBigDecimal().doubleValue()));
        comps[REAL] = (self, real, comp) -> {
            double selfDbl = self.getDouble();
            float otherFlt = real.getFloat();
            return compareDoubles(selfDbl, otherFlt, () -> Double.compare(selfDbl, otherFlt));
        };
        comps[DOUBLE] = (self, doublePrecision, comp) -> compareDoubles(self.getDouble(), doublePrecision.getDouble(), () -> Double.compare(self.getDouble(), doublePrecision.getDouble()));
        comps[DECIMAL] = (self, decimal, comp) -> compareDoubleLhs(self.getDouble(), () -> BigDecimal.valueOf(self.getDouble()).compareTo(decimal.getBigDecimal()));
        return comps;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillDecimalComparator(DatumComparison[] comps) {
        comps[TINYINT] = (self, tinyInt, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(tinyInt.getByte()));
        comps[SMALLINT] = (self, smallInt, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(smallInt.getShort()));
        comps[INTEGER] = (self, intNum, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(intNum.getInt()));
        comps[BIGINT] = (self, bigInt, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(bigInt.getLong()));
        comps[NUMERIC] = (self, numeric, comp) -> self.getBigDecimal().compareTo(numeric.getBigDecimal());
        comps[REAL] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> self.getBigDecimal().compareTo(BigDecimal.valueOf(real.getFloat())));
        comps[DOUBLE] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> self.getBigDecimal().compareTo(BigDecimal.valueOf(doublePrecision.getDouble())));
        comps[DECIMAL] = (self, decimal, comp) -> self.getBigDecimal().compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillDateComparator(DatumComparison[] comps) {
        comps[DATE] = (self, date, comp) -> self.getLocalDate().compareTo(date.getLocalDate());
        return comps;
    }

    /**
     * Used for both {@link PType#TIME} and {@link PType#TIMEZ}
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillTimeComparator(DatumComparison[] comps) {
        comps[TIME] = (self, time, comp) -> self.getLocalTime().compareTo(time.getLocalTime());
        comps[TIMEZ] = (self, time, comp) -> self.getOffsetTime().compareTo(time.getOffsetTime());
        return comps;
    }

    /**
     * Used for both {@link PType#TIMESTAMP} and {@link PType#TIMESTAMPZ}
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillTimestampComparator(DatumComparison[] comps) {
        comps[TIMESTAMP] = (self, timestamp, comp) -> self.getLocalDateTime().compareTo(timestamp.getLocalDateTime());
        comps[TIMESTAMPZ] = (self, timestamp, comp) -> self.getOffsetDateTime().compareTo(timestamp.getOffsetDateTime());
        return comps;
    }

    /**
     * Used for {@link PType#STRING}, {@link PType#CHAR}, {@link PType#VARCHAR}.
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillStringComparator(DatumComparison[] comps) {
        comps[STRING] = (self, string, comp) -> self.getString().compareTo(string.getString());
        comps[CHAR] = (self, string, comp) -> self.getString().compareTo(string.getString());
        comps[PType.VARCHAR] = (self, string, comp) -> self.getString().compareTo(string.getString());
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillListComparator(DatumComparison[] comps) {
        comps[ARRAY] = (self, list, comp) -> compareOrdered(self.iterator(), list.iterator(), comp);
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillBagComparator(DatumComparison[] comps) {
        comps[BAG] = DatumComparator::compareUnordered;
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillBooleanComparator(DatumComparison[] comps) {
        comps[BOOL] = (self, bool, comp) -> Boolean.compare(self.getBoolean(), bool.getBoolean());
        return comps;
    }

    private static class ByteComparator implements Comparator<Byte>{
        @Override
        public int compare(Byte o1, Byte o2) {
            return Byte.compare(o1, o2);
        }
    }

    private static class ByteIterator implements Iterator<Byte> {
        private int index = 0;
        private final byte[] bytes;

        ByteIterator(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean hasNext() {
            return index < bytes.length;
        }

        @Override
        public Byte next() {
            return bytes[index++];
        }
    }

    /**
     * Used for both {@link PType#BLOB} and {@link PType#CLOB}.
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillLobComparator(DatumComparison[] comps) {
        comps[BLOB] = (self, blob, comp) -> compareArray(self.getBytes(), blob.getBytes());
        comps[CLOB] = (self, blob, comp) -> compareArray(self.getBytes(), blob.getBytes());
        return comps;
    }

    /**
     * Used for both {@link PType#STRUCT} and {@link PType#ROW}.
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType#code()}.
     * @return the modified array
     */
    @SuppressWarnings({"UnusedReturnValue"})
    private static DatumComparison[] fillStructComparator(DatumComparison[] comps) {
        comps[STRUCT] = (self, struct, comp) -> compareUnordered(new DatumFieldIterable(self), new DatumFieldIterable(struct), new FieldComparator(comp));
        comps[PType.ROW] = (self, row, comp) -> compareOrdered(self.getFields(), row.getFields(), new FieldComparator(comp));
        return comps;
    }

    private static class FieldComparator implements Comparator<Field> {

        private final Comparator<Datum> comparator;

        FieldComparator(Comparator<Datum> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(Field o1, Field o2) {
            int cmpKey = o1.getName().compareTo(o2.getName());
            if (cmpKey != 0) {
                return cmpKey;
            }
            return comparator.compare(o1.getValue(), o2.getValue());
        }
    }

    /**
     * Converts the {@link Datum#getFields()} API into an iterable by wrapping the {@link Datum} itself.
     */
    private static class DatumFieldIterable implements Iterable<Field> {
        private final Datum datum;
        DatumFieldIterable(Datum datum) {
            this.datum = datum;
        }
        @NotNull
        @Override
        public Iterator<Field> iterator() {
            return datum.getFields();
        }
    }

    private static int compareArray(byte[] l, byte[] r) {
        Iterator<Byte> lIter = new ByteIterator(l);
        Iterator<Byte> rIter = new ByteIterator(r);
        return compareOrdered(lIter, rIter, new ByteComparator());
    }

    private static <T> int compareOrdered(Iterator<T> lIter, Iterator<T> rIter, Comparator<T> elementComparator) {
        while (lIter.hasNext() && rIter.hasNext()) {
            T lVal = lIter.next();
            T rVal = rIter.next();
            int result = elementComparator.compare(lVal, rVal);
            if (result != 0) {
                return result;
            }
        }
        if (lIter.hasNext()) {
            return GREATER;
        }
        if (rIter.hasNext()) {
            return LESS;
        }
        return EQUAL;
    }

    private static <T> int compareUnordered(Iterable<T> l, Iterable<T> r, Comparator<T> elementComparator) {
        List<T> lhsList = new ArrayList<>();
        l.forEach(lhsList::add);
        List<T> rhsList = new ArrayList<>();
        r.forEach(rhsList::add);
        lhsList.sort(elementComparator);
        rhsList.sort(elementComparator);
        return compareOrdered(lhsList.iterator(), rhsList.iterator(), elementComparator);

    }

    /**
     * @param lhs the original left-hand-side argument's type of {@link Comparator#compare(Object, Object)}.
     * @return an array that indicates the type precedence output of {@code lhs.compare(rhs)}. This uses the
     * {@link PType#code() to make O(1) judgements. This array will be further modified by type-specific
     * methods.
     * @see #fillTinyIntComparator(DatumComparison[])
     * @see #fillSmallIntComparator(DatumComparison[])
     */
    @NotNull
    private static DatumComparison[] initializeComparatorArray(int lhs) {
        DatumComparison[] array = new DatumComparison[TYPE_KINDS_LENGTH];
        for (int i = 0; i < TYPE_KINDS_LENGTH; i++) {
            int rhs = TYPE_KINDS[i];
            int lhsPrecedence = TYPE_PRECEDENCE.getOrDefault(lhs, -1);
            int rhsPrecedence = TYPE_PRECEDENCE.getOrDefault(rhs, -1);
            if (lhsPrecedence < 0) {
                throw new IllegalStateException("No precedence set for type: " + lhs);
            }
            if (rhsPrecedence < 0) {
                throw new IllegalStateException("No precedence set for type: " + rhs);
            }
            int typeComparison = Integer.compare(lhsPrecedence, rhsPrecedence);
            array[i] = (self, other, comp) -> typeComparison;
        }
        return array;
    }

    /**
     * Represents the act of comparing two {@link Datum}s. This method also passes a {@link Comparator} for
     * collections and structs.
     */
    @FunctionalInterface
    interface DatumComparison {
        int apply(Datum lhs, Datum rhs, Comparator<Datum> comparator);
    }
}

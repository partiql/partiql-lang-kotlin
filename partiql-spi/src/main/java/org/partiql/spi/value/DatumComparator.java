package org.partiql.spi.value;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.PType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This class allows for the comparison between two {@link Datum}s. This is internally implemented by constructing
 * a comparison table, where each cell contains a reference to a {@link DatumComparison} to compute the comparison.
 * The table's rows and columns are indexed by the {@link PType.Kind#ordinal()}. The first dimension matches the
 * left-hand-side's type of {@link #compare(Datum lhs, Datum rhs)}. The second dimension matches the right-hand-side's
 * type of {@link #compare(Datum lhs, Datum rhs)}. As such, this implementation allows for O(1) comparison of scalars.
 */
abstract class DatumComparator implements Comparator<Datum> {

    private static final int EQUAL = 0;

    private static final int LESS = -1;

    private static final int GREATER = 1;

    @NotNull
    private static final PType.Kind[] TYPE_KINDS = PType.Kind.values();

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
    private static final Map<PType.Kind, Integer> TYPE_PRECEDENCE = initializeTypePrecedence();

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
        // Check for NULL/MISSING
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

        // Invoke the Comparison Table
        int lhsKind = lhs.getType().getKind().ordinal();
        int rhsKind = rhs.getType().getKind().ordinal();
        return COMPARISON_TABLE[lhsKind][rhsKind].apply(lhs, rhs, this);
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
    @SuppressWarnings("deprecation")
    private static Map<PType.Kind, Integer> initializeTypePrecedence() {
        Map<PType.Kind, Integer> precedence = new HashMap<>();
        // Boolean Type
        precedence.put(PType.Kind.BOOL, 0);
        // Number Types
        precedence.put(PType.Kind.TINYINT, 1);
        precedence.put(PType.Kind.SMALLINT, 1);
        precedence.put(PType.Kind.INTEGER, 1);
        precedence.put(PType.Kind.BIGINT, 1);
        precedence.put(PType.Kind.NUMERIC, 1);
        precedence.put(PType.Kind.DECIMAL, 1);
        precedence.put(PType.Kind.DECIMAL_ARBITRARY, 1);
        precedence.put(PType.Kind.REAL, 1);
        precedence.put(PType.Kind.DOUBLE, 1);
        // Date Type
        precedence.put(PType.Kind.DATE, 2);
        // Time Type
        precedence.put(PType.Kind.TIMEZ, 3);
        precedence.put(PType.Kind.TIME, 3);
        // Timestamp Types
        precedence.put(PType.Kind.TIMESTAMPZ, 4);
        precedence.put(PType.Kind.TIMESTAMP, 4);
        // Text Types
        precedence.put(PType.Kind.CHAR, 5);
        precedence.put(PType.Kind.VARCHAR, 5);
        precedence.put(PType.Kind.STRING, 5);
        precedence.put(PType.Kind.SYMBOL, 5);
        // LOB Types
        precedence.put(PType.Kind.CLOB, 6);
        precedence.put(PType.Kind.BLOB, 6);
        // Array Type
        precedence.put(PType.Kind.ARRAY, 7);
        // Sexp Type
        precedence.put(PType.Kind.SEXP, 8);
        // Tuple Type
        precedence.put(PType.Kind.ROW, 9);
        precedence.put(PType.Kind.STRUCT, 9);
        // Bag Type
        precedence.put(PType.Kind.BAG, 10);
        // OTHER
        precedence.put(PType.Kind.DYNAMIC, 100);
        precedence.put(PType.Kind.UNKNOWN, 100);
        precedence.put(PType.Kind.VARIANT, 100);
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
     * @see #initializeComparatorArray(PType.Kind)
     * @see #fillIntComparator(DatumComparison[])
     */
    @SuppressWarnings("deprecation")
    private static DatumComparison[][] initializeComparators() {
        // Initialize Table
        DatumComparison[][] table = new DatumComparison[TYPE_KINDS_LENGTH][TYPE_KINDS_LENGTH];
        for (int i = 0; i < TYPE_KINDS_LENGTH; i++) {
            @SuppressWarnings("ConstantConditions")
            PType.Kind kind = TYPE_KINDS[i];
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
                    fillIntArbitraryComparator(row);
                    break;
                case DECIMAL:
                case DECIMAL_ARBITRARY:
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
                case SYMBOL:
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
                case SEXP:
                    fillSexpComparator(row);
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

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillTinyIntComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> Byte.compare(self.getByte(), tinyInt.getByte());
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> Short.compare(self.getByte(), smallInt.getShort());
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> Integer.compare(self.getByte(), intNum.getInt());
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> Long.compare(self.getByte(), bigInt.getLong());
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> BigInteger.valueOf(self.getByte()).compareTo(intArbitrary.getBigInteger());
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getByte(), real.getFloat()));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getByte(), doublePrecision.getDouble()));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getByte()).compareTo(decimal.getBigDecimal());
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getByte()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillSmallIntComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> Short.compare(self.getShort(), tinyInt.getByte());
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> Short.compare(self.getShort(), smallInt.getShort());
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> Integer.compare(self.getShort(), intNum.getInt());
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> Long.compare(self.getShort(), bigInt.getLong());
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> BigInteger.valueOf(self.getShort()).compareTo(intArbitrary.getBigInteger());
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getShort(), real.getFloat()));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getShort(), doublePrecision.getDouble()));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getShort()).compareTo(decimal.getBigDecimal());
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getShort()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillIntComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> Integer.compare(self.getInt(), tinyInt.getByte());
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> Integer.compare(self.getInt(), smallInt.getShort());
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> Integer.compare(self.getInt(), intNum.getInt());
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> Long.compare(self.getInt(), bigInt.getLong());
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> BigInteger.valueOf(self.getInt()).compareTo(intArbitrary.getBigInteger());
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getInt(), real.getFloat()));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getInt(), doublePrecision.getDouble()));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getInt()).compareTo(decimal.getBigDecimal());
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getInt()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillBigIntComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> Long.compare(self.getLong(), tinyInt.getByte());
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> Long.compare(self.getLong(), smallInt.getShort());
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> Long.compare(self.getLong(), intNum.getInt());
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> Long.compare(self.getLong(), bigInt.getLong());
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> BigInteger.valueOf(self.getLong()).compareTo(intArbitrary.getBigInteger());
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> Float.compare(self.getLong(), real.getFloat()));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> Double.compare(self.getLong(), doublePrecision.getDouble()));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getLong()).compareTo(decimal.getBigDecimal());
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> BigDecimal.valueOf(self.getLong()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillIntArbitraryComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> self.getBigInteger().compareTo(BigInteger.valueOf(tinyInt.getByte()));
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> self.getBigInteger().compareTo(BigInteger.valueOf(smallInt.getShort()));
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> self.getBigInteger().compareTo(BigInteger.valueOf(intNum.getInt()));
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> self.getBigInteger().compareTo(BigInteger.valueOf(bigInt.getLong()));
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> self.getBigInteger().compareTo(intArbitrary.getBigInteger());
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> new BigDecimal(self.getBigInteger()).compareTo(BigDecimal.valueOf(real.getFloat())));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> new BigDecimal(self.getBigInteger()).compareTo(BigDecimal.valueOf(doublePrecision.getDouble())));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> new BigDecimal(self.getBigInteger()).compareTo(decimal.getBigDecimal());
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> new BigDecimal(self.getBigInteger()).compareTo(decimal.getBigDecimal());
        return comps;
    }

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillRealComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), tinyInt.getByte()));
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), smallInt.getShort()));
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), intNum.getInt()));
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), bigInt.getLong()));
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> compareDoubleLhs(self.getFloat(), () -> Float.compare(self.getFloat(), intArbitrary.getBigInteger().floatValue()));
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) -> compareDoubles(self.getFloat(), real.getFloat(), () -> Float.compare(self.getFloat(), real.getFloat()));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> {
            float selfFlt = self.getFloat();
            double otherDbl = doublePrecision.getDouble();
            return compareDoubles(selfFlt, otherDbl, () -> Double.compare(selfFlt, otherDbl));
        };
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> compareDoubleLhs(self.getFloat(), () -> BigDecimal.valueOf(self.getFloat()).compareTo(decimal.getBigDecimal()));
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> compareDoubleLhs(self.getFloat(), () -> BigDecimal.valueOf(self.getFloat()).compareTo(decimal.getBigDecimal()));
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

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillDoubleComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), tinyInt.getByte()));
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), smallInt.getShort()));
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), intNum.getInt()));
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), bigInt.getLong()));
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> compareDoubleLhs(self.getDouble(), () -> Double.compare(self.getDouble(), intArbitrary.getBigInteger().doubleValue()));
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) -> {
            double selfDbl = self.getDouble();
            float otherFlt = real.getFloat();
            return compareDoubles(selfDbl, otherFlt, () -> Double.compare(selfDbl, otherFlt));
        };
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubles(self.getDouble(), doublePrecision.getDouble(), () -> Double.compare(self.getDouble(), doublePrecision.getDouble()));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> compareDoubleLhs(self.getDouble(), () -> BigDecimal.valueOf(self.getDouble()).compareTo(decimal.getBigDecimal()));
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> compareDoubleLhs(self.getDouble(), () -> BigDecimal.valueOf(self.getDouble()).compareTo(decimal.getBigDecimal()));
        return comps;
    }

    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillDecimalComparator(DatumComparison[] comps) {
        comps[PType.Kind.TINYINT.ordinal()] = (self, tinyInt, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(tinyInt.getByte()));
        comps[PType.Kind.SMALLINT.ordinal()] = (self, smallInt, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(smallInt.getShort()));
        comps[PType.Kind.INTEGER.ordinal()] = (self, intNum, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(intNum.getInt()));
        comps[PType.Kind.BIGINT.ordinal()] = (self, bigInt, comp) -> self.getBigDecimal().compareTo(BigDecimal.valueOf(bigInt.getLong()));
        comps[PType.Kind.NUMERIC.ordinal()] = (self, intArbitrary, comp) -> self.getBigDecimal().compareTo(new BigDecimal(intArbitrary.getBigInteger()));
        comps[PType.Kind.REAL.ordinal()] = (self, real, comp) ->  compareDoubleRhs(real.getFloat(), () -> self.getBigDecimal().compareTo(BigDecimal.valueOf(real.getFloat())));
        comps[PType.Kind.DOUBLE.ordinal()] = (self, doublePrecision, comp) -> compareDoubleRhs(doublePrecision.getDouble(), () -> self.getBigDecimal().compareTo(BigDecimal.valueOf(doublePrecision.getDouble())));
        comps[PType.Kind.DECIMAL.ordinal()] = (self, decimal, comp) -> self.getBigDecimal().compareTo(decimal.getBigDecimal());
        comps[PType.Kind.DECIMAL_ARBITRARY.ordinal()] = (self, decimal, comp) -> self.getBigDecimal().compareTo(decimal.getBigDecimal());
        return comps;
    }

    private static DatumComparison[] fillDateComparator(DatumComparison[] comps) {
        comps[PType.Kind.DATE.ordinal()] = (self, date, comp) -> self.getDate().compareTo(date.getDate());
        return comps;
    }

    /**
     * Used for both {@link PType.Kind#TIME} and {@link PType.Kind#TIMEZ}
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    private static DatumComparison[] fillTimeComparator(DatumComparison[] comps) {
        comps[PType.Kind.TIME.ordinal()] = (self, time, comp) -> self.getTime().compareTo(time.getTime());
        comps[PType.Kind.TIMEZ.ordinal()] = (self, time, comp) -> self.getTime().compareTo(time.getTime());
        return comps;
    }

    /**
     * Used for both {@link PType.Kind#TIMESTAMP} and {@link PType.Kind#TIMESTAMPZ}
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    private static DatumComparison[] fillTimestampComparator(DatumComparison[] comps) {
        comps[PType.Kind.TIMESTAMPZ.ordinal()] = (self, timestamp, comp) -> self.getTimestamp().compareTo(timestamp.getTimestamp());
        comps[PType.Kind.TIMESTAMP.ordinal()] = (self, timestamp, comp) -> self.getTimestamp().compareTo(timestamp.getTimestamp());
        return comps;
    }

    /**
     * Used for {@link PType.Kind#STRING}, {@link PType.Kind#CHAR}, {@link PType.Kind#VARCHAR}, and {@link PType.Kind#SYMBOL}.
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillStringComparator(DatumComparison[] comps) {
        comps[PType.Kind.STRING.ordinal()] = (self, string, comp) -> self.getString().compareTo(string.getString());
        comps[PType.Kind.CHAR.ordinal()] = (self, string, comp) -> self.getString().compareTo(string.getString());
        comps[PType.Kind.VARCHAR.ordinal()] = (self, string, comp) -> self.getString().compareTo(string.getString());
        comps[PType.Kind.SYMBOL.ordinal()] = (self, string, comp) -> self.getString().compareTo(string.getString());
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    private static DatumComparison[] fillListComparator(DatumComparison[] comps) {
        comps[PType.Kind.ARRAY.ordinal()] = (self, list, comp) -> compareOrdered(self.iterator(), list.iterator(), comp);
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillSexpComparator(DatumComparison[] comps) {
        comps[PType.Kind.SEXP.ordinal()] = (self, list, comp) -> compareOrdered(self.iterator(), list.iterator(), comp);
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    private static DatumComparison[] fillBagComparator(DatumComparison[] comps) {
        comps[PType.Kind.BAG.ordinal()] = DatumComparator::compareUnordered;
        return comps;
    }

    /**
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    private static DatumComparison[] fillBooleanComparator(DatumComparison[] comps) {
        comps[PType.Kind.BOOL.ordinal()] = (self, bool, comp) -> Boolean.compare(self.getBoolean(), bool.getBoolean());
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
     * Used for both {@link PType.Kind#BLOB} and {@link PType.Kind#CLOB}.
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillLobComparator(DatumComparison[] comps) {
        comps[PType.Kind.BLOB.ordinal()] = (self, blob, comp) -> compareArray(self.getBytes(), blob.getBytes());
        comps[PType.Kind.CLOB.ordinal()] = (self, blob, comp) -> compareArray(self.getBytes(), blob.getBytes());
        return comps;
    }

    /**
     * Used for both {@link PType.Kind#STRUCT} and {@link PType.Kind#ROW}.
     * @param comps the array of {@link DatumComparison} to modify. Each {@link DatumComparison} is indexed by the other
     * {@link Datum}'s {@link PType.Kind#ordinal()}.
     * @return the modified array
     */
    @SuppressWarnings("deprecation")
    private static DatumComparison[] fillStructComparator(DatumComparison[] comps) {
        comps[PType.Kind.STRUCT.ordinal()] = (self, struct, comp) -> compareUnordered(new DatumFieldIterable(self), new DatumFieldIterable(struct), new FieldComparator(comp));
        comps[PType.Kind.ROW.ordinal()] = (self, row, comp) -> compareOrdered(self.getFields(), row.getFields(), new FieldComparator(comp));
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
     * {@link PType.Kind#ordinal()} to make O(1) judgements. This array will be further modified by type-specific
     * methods.
     * @see #fillTinyIntComparator(DatumComparison[])
     * @see #fillSmallIntComparator(DatumComparison[])
     */
    @NotNull
    private static DatumComparison[] initializeComparatorArray(@NotNull PType.Kind lhs) {
        DatumComparison[] array = new DatumComparison[TYPE_KINDS_LENGTH];
        for (int i = 0; i < TYPE_KINDS_LENGTH; i++) {
            PType.Kind rhs = TYPE_KINDS[i];
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

package org.partiql.ast;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Represents an interval qualifier in the AST.
 * </p>
 */
public abstract class IntervalQualifier extends AstNode {

    /**
     * <p>
     * Represents an interval qualifier that contains a range of fields. Syntactically,
     * this is representing by {@code start_field TO end_field}. For example: {@code YEAR (5) TO MONTH}.
     * </p>
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Range extends IntervalQualifier {
        @NotNull
        private final DatetimeField startField;

        @Nullable
        private final Integer leadingFieldPrecision;

        @NotNull
        private final DatetimeField endField;

        @Nullable
        private final Integer endFieldFractionalPrecision;

        /**
         * Returns the start field of the range.
         * @return the start field of the range
         */
        @NotNull
        public DatetimeField getStartField() {
            return this.startField;
        }

        /**
         * Returns the precision of the leading field.
         * @return the precision of the leading field
         */
        @Nullable
        public Integer getStartFieldPrecision() {
            return this.leadingFieldPrecision;
        }

        /**
         * Returns the end field of the range.
         * @return the end field of the range
         */
        @NotNull
        public DatetimeField getEndField() {
            return this.endField;
        }

        /**
         * Returns the fractional precision of the end field. This may only be present if the end
         * field is {@link DatetimeField#SECOND}.
         * @return the fractional precision of the end field
         */
        @Nullable
        public Integer getEndFieldFractionalPrecision() {
            return this.endFieldFractionalPrecision;
        }

        /**
         * Constructs a new Range instance.
         * @param startField the start field of the range
         * @param startFieldPrecision the precision of the leading field
         * @param endField the end field of the range
         * @param endFieldFractionalPrecision the fractional precision of the end field
         */
        public Range(
                @NotNull DatetimeField startField,
                @Nullable Integer startFieldPrecision,
                @NotNull DatetimeField endField,
                @Nullable Integer endFieldFractionalPrecision
        ) throws IllegalArgumentException {
            checkStartField(startField, startFieldPrecision);
            checkEndField(endField, endFieldFractionalPrecision);
            this.startField = startField;
            this.leadingFieldPrecision = startFieldPrecision;
            this.endField = endField;
            this.endFieldFractionalPrecision = endFieldFractionalPrecision;
        }

        /**
         * Checks that the start field utilizes a valid datetime field and that the leading field precision is valid.
         * @param startField the start field of the range
         * @param startFieldPrecision the precision of the leading field
         * @throws IllegalArgumentException when the start field is not a valid datetime field or the leading field precision is not valid
         */
        private static void checkStartField(
                @NotNull DatetimeField startField,
                @Nullable Integer startFieldPrecision
        ) throws IllegalArgumentException {
            int startCode = startField.code();
            if (!ALLOWABLE_START_FIELDS.contains(startCode)) {
                throw new IllegalArgumentException("Invalid interval start field: " + startField.name());
            }
            if (startFieldPrecision != null && startFieldPrecision <= 0) {
                throw new IllegalArgumentException("Invalid leading field precision" + startFieldPrecision);
            }
        }

        /**
         * Checks that the end field utilizes a valid datetime field and that the fractional seconds precision is valid.
         * @param endField the end field of the range
         * @param endFieldPrecision the fractional seconds precision of the end field
         * @throws IllegalArgumentException when the end field is not a valid datetime field or the fractional seconds precision is not valid
         */
        private static void checkEndField(
                @NotNull DatetimeField endField,
                @Nullable Integer endFieldPrecision
        ) throws IllegalArgumentException {
            int code = endField.code();
            if (!ALLOWABLE_END_FIELDS.contains(code)) {
                throw new IllegalArgumentException("Invalid interval end field: " + endField.name());
            }
            if (endFieldPrecision != null && code != DatetimeField.SECOND) {
                throw new IllegalArgumentException("Cannot specify fractional seconds precision for interval end field: " + endField.name());
            }
            if (endFieldPrecision != null && endFieldPrecision < 0) {
                throw new IllegalArgumentException("Invalid end field precision" + endFieldPrecision);
            }
        }

        private static Set<Integer> ALLOWABLE_START_FIELDS = new HashSet<Integer>() {{
            add(DatetimeField.YEAR);
            add(DatetimeField.MONTH);
            add(DatetimeField.DAY);
            add(DatetimeField.HOUR);
            add(DatetimeField.MINUTE);
        }};

        private static Set<Integer> ALLOWABLE_END_FIELDS = new HashSet<Integer>() {{
            add(DatetimeField.YEAR);
            add(DatetimeField.MONTH);
            add(DatetimeField.DAY);
            add(DatetimeField.HOUR);
            add(DatetimeField.MINUTE);
            add(DatetimeField.SECOND);
        }};

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(startField);
            kids.add(endField);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitIntervalQualifierRange(this, ctx);
        }
    }

    /**
     * <p>
     * Represents an interval qualifier that contains a single field. Syntactically,
     * this is representing by {@code field} (precision). For example: {@code DAY (5)}
     * </p>
     */
    @lombok.Builder(builderClassName = "Builder")
    @EqualsAndHashCode(callSuper = false)
    public static final class Single extends IntervalQualifier {
        @NotNull
        private final DatetimeField field;

        @Nullable
        private final Integer precision;

        @Nullable
        private final Integer fractionalPrecision;

        /**
         * Returns the field of the interval.
         * @return the field of the interval
         */
        @NotNull
        public DatetimeField getField() {
            return this.field;
        }

        /**
         * Returns the precision of the interval.
         * @return the precision of the interval
         */
        @Nullable
        public Integer getPrecision() {
            return this.precision;
        }

        /**
         * Returns the fractional precision of the interval.
         * @return the fractional precision of the interval
         */
        @Nullable
        public Integer getFractionalPrecision() {
            return this.fractionalPrecision;
        }

        /**
         * Constructs a new Single instance.
         * @param field the field of the interval
         * @param precision the precision of the interval
         * @param fractionalPrecision the fractional precision of the interval
         * @throws IllegalArgumentException when the field is not a valid datetime field or the precision or fractional precision is not valid
         */
        public Single(
                @NotNull DatetimeField field,
                @Nullable Integer precision,
                @Nullable Integer fractionalPrecision
        ) throws IllegalArgumentException {
            checkInputs(field, precision, fractionalPrecision);
            this.field = field;
            this.precision = precision;
            this.fractionalPrecision = fractionalPrecision;
        }

        private static void checkInputs(@NotNull DatetimeField field, @Nullable Integer precision, @Nullable Integer scale) {
            int code = field.code();
            if (!ALLOWABLE_FIELDS.contains(code)) {
                throw new IllegalArgumentException("Invalid interval field: " + field.name());
            }
            if (precision != null && precision <= 0) {
                throw new IllegalArgumentException("Invalid precision" + precision);
            }
            if (scale != null && code != DatetimeField.SECOND) {
                throw new IllegalArgumentException("Cannot specify fractional seconds precision for interval field: " + field.name());
            }
            if (scale != null && scale < 0) {
                throw new IllegalArgumentException("Invalid scale" + scale);
            }
        }

        private static Set<Integer> ALLOWABLE_FIELDS = new HashSet<Integer>() {{
            add(DatetimeField.YEAR);
            add(DatetimeField.MONTH);
            add(DatetimeField.DAY);
            add(DatetimeField.HOUR);
            add(DatetimeField.MINUTE);
            add(DatetimeField.SECOND);
        }};

        @NotNull
        @Override
        public List<AstNode> getChildren() {
            List<AstNode> kids = new ArrayList<>();
            kids.add(field);
            return kids;
        }

        @Override
        public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
            return visitor.visitIntervalQualifierSingle(this, ctx);
        }
    }
}

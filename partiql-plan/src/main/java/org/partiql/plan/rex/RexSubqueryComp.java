package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical subquery comparison expression abstract base class.
 * <p>
 * See SQL-99 <comparison predicate> and <quantified comparison predicate>.
 */
public abstract class RexSubqueryComp extends RexBase {

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return collection comparison arguments (not children).
     */
    @NotNull
    public abstract List<Rex> getArgs();

    /**
     * @return subquery comparison operator
     */
    @NotNull
    public abstract Comparison getComparison();

    /**
     * @return subquery comparison quantifier
     */
    @NotNull
    public abstract Quantifier getQuantifier();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.bool());
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSubqueryComp(this, ctx);
    }

    /**
     * SQL <comp op> for use in the <quantified comparison predicate>.
     */
    public static class Comparison extends org.partiql.spi.Enum {

        private Comparison(int code) {
            super(code);
        }

        public static int UNKNOWN = 0;
        public static int EQ = 1;
        public static int NE = 2;
        public static int LT = 3;
        public static int LE = 4;
        public static int GT = 5;
        public static int GE = 6;

        @NotNull
        public static Comparison EQ() {
            return new Comparison(EQ);
        }

        @NotNull
        public static Comparison NE() {
            return new Comparison(NE);
        }

        @NotNull
        public static Comparison LT() {
            return new Comparison(LT);
        }

        @NotNull
        public static Comparison LE() {
            return new Comparison(LE);
        }

        @NotNull
        public static Comparison GT() {
            return new Comparison(GT);
        }

        @NotNull
        public static Comparison GE() {
            return new Comparison(GE);
        }
    }

    /**
     * SQL <quantifier> for use in the <quantified comparison predicate>.
     */
    public static class Quantifier extends org.partiql.spi.Enum {

        private Quantifier(int code) {
            super(code);
        }

        public static int UNKNOWN = 0;
        public static int ANY = 1;
        public static int ALL = 2;
        public static int SOME = 3;

        @NotNull
        public static Quantifier ANY() {
            return new Quantifier(ANY);
        }

        @NotNull
        public static Quantifier ALL() {
            return new Quantifier(ALL);
        }

        @NotNull
        public static Quantifier SOME() {
            return new Quantifier(SOME);
        }
    }
}

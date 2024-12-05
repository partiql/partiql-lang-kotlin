package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;
import org.partiql.spi.Enum;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical subquery comparison expression abstract base class.
 * <p>
 * See SQL-99 <comparison predicate> and <quantified comparison predicate>.
 */
public abstract class RexSubqueryComp extends RexBase {

    /**
     * @return new RexSubqueryComp instance
     */
    @NotNull
    public static RexSubqueryComp create(
            @NotNull Rel input,
            @NotNull List<Rex> args,
            @NotNull Comparison comparison,
            @NotNull Quantifier quantifier
    ) {
        return new Impl(input, args, comparison, quantifier);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return collection comparison arguments (not operands).
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
        return RexType.of(PType.bool());
    }

    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitSubqueryComp(this, ctx);
    }

    /**
     * SQL <comp op> for use in the <quantified comparison predicate>.
     */
    public static class Comparison extends Enum {

        private Comparison(int code) {
            super(code);
        }

        public static final int UNKNOWN = 0;
        public static final int EQ = 1;
        public static final int NE = 2;
        public static final int LT = 3;
        public static final int LE = 4;
        public static final int GT = 5;
        public static final int GE = 6;

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
    public static class Quantifier extends Enum {

        private Quantifier(int code) {
            super(code);
        }

        public static final int UNKNOWN = 0;
        public static final int ANY = 1;
        public static final int ALL = 2;
        public static final int SOME = 3;

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

    private static class Impl extends RexSubqueryComp {

        private final Rel input;
        private final List<Rex> args;
        private final Comparison comparison;
        private final Quantifier quantifier;

        private Impl(Rel input, List<Rex> args, Comparison comparison, Quantifier quantifier) {
            this.input = input;
            this.args = args;
            this.comparison = comparison;
            this.quantifier = quantifier;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }

        @NotNull
        @Override
        public Comparison getComparison() {
            return comparison;
        }

        @NotNull
        @Override
        public Quantifier getQuantifier() {
            return quantifier;
        }
    }
}

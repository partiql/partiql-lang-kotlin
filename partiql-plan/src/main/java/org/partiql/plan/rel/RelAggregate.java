package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.function.Aggregation;

import java.util.List;

/**
 * The logical aggregation abstract base class.
 */
public abstract class RelAggregate extends RelBase {

    // TODO GROUP STRATEGY: https://github.com/partiql/partiql-lang-kotlin/issues/1664

    /**
     * @return the input (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    @NotNull
    public abstract List<Measure> getMeasures();

    @NotNull
    public abstract List<Rex> getGroups();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitAggregate(this, ctx);
    }

    /**
     * An aggregation function along with its arguments and any additional filters (e.g. DISTINCT).
     */
    public static class Measure {

        private final Aggregation agg;
        private final List<Rex> args;
        private final Boolean distinct;

        public Measure(Aggregation agg, List<Rex> args, Boolean distinct) {
            this.agg = agg;
            this.args = args;
            this.distinct = distinct;
        }

        @NotNull
        public Aggregation getAgg() {
            return agg;
        }

        @NotNull
        public List<Rex> getArgs() {
            return args;
        }

        @NotNull
        public Boolean isDistinct() {
            return distinct;
        }
    }
}

package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.function.Aggregation;

import java.util.Collection;

/**
 * Interface for an aggregation operator.
 * <br>
 * TODO GROUP STRATEGY <a href="https://github.com/partiql/partiql-lang-kotlin/issues/1664">ISSUE</a>
 */
public interface RelAggregate extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Collection<Measure> getMeasures();

    @NotNull
    public Collection<Rex> getGroups();

    @NotNull
    public Collection<Operator> getChildren();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitAggregate(this, ctx);
    }

    /**
     * An aggregation function along with its arguments and any additional filters (e.g. DISTINCT).
     */
    public class Measure {

        private final Aggregation agg;
        private final Collection<Rex> args;
        private final Boolean distinct;

        public Measure(Aggregation agg, Collection<Rex> args, Boolean distinct) {
            this.agg = agg;
            this.args = args;
            this.distinct = distinct;
        }

        @NotNull
        public Aggregation getAgg() {
            return agg;
        }

        @NotNull
        public Collection<Rex> getArgs() {
            return args;
        }

        @NotNull
        public Boolean isDistinct() {
            return distinct;
        }
    }
}

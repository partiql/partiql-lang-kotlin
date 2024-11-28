package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.spi.catalog.Table;

import java.util.List;

/**
 * Global variable references e.g. tables and views.
 */
public abstract class RexTable extends RexBase {

    /**
     * @return new RexTable instance
     */
    @NotNull
    public static RexTable create(@NotNull Table table) {
        return new Impl(table);
    }

    /**
     * @return the table implementation.
     */
    public abstract Table getTable();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(getTable().getSchema());
    }

    @Override
    protected final List<Operator> children() {
        return List.of();
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitTable(this, ctx);
    }

    private static class Impl extends RexTable {

        private final Table table;

        private Impl(Table table) {
            this.table = table;
        }

        @Override
        public Table getTable() {
            return table;
        }
    }
}

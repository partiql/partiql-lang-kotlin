package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
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
        return RexType.of(getTable().getSchema());
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
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

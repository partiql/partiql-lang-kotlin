package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * A reference to a table by catalog and table IDs, resolved lazily at execution time.
 */
public abstract class RexTableRef extends RexBase {

    /**
     * Creates a new RexTableRef instance.
     *
     * @param catalogId the catalog identifier assigned during planning
     * @param tableId   the table identifier within the catalog
     * @param schema    the table's schema known at plan time
     * @return new RexTableRef instance
     */
    @NotNull
    public static RexTableRef create(int catalogId, int tableId, @NotNull PType schema) {
        return new Impl(catalogId, tableId, schema);
    }

    /**
     * Returns the catalog identifier.
     * @return catalog identifier
     */
    public abstract int getCatalogId();

    /**
     * Returns the table identifier within the catalog.
     * @return table identifier
     */
    public abstract int getTableId();

    @NotNull
    @Override
    protected final List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitTableRef(this, ctx);
    }

    private static class Impl extends RexTableRef {

        private final int catalogId;
        private final int tableId;
        private final PType schema;

        private Impl(int catalogId, int tableId, PType schema) {
            this.catalogId = catalogId;
            this.tableId = tableId;
            this.schema = schema;
        }

        @NotNull
        @Override
        protected RexType type() {
            return RexType.of(schema);
        }

        @Override
        public int getCatalogId() {
            return catalogId;
        }

        @Override
        public int getTableId() {
            return tableId;
        }
    }
}

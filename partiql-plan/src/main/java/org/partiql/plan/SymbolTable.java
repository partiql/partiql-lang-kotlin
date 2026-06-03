package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.catalog.Name;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * A symbol table mapping plan-assigned integer IDs to catalog and table names.
 * <p>
 * Returned alongside the {@link Plan} from the planner. Database owners use this to understand what each
 * integer ID in the plan represents and to build their {@link org.partiql.spi.catalog.ExecutionCatalog} accordingly.
 * <p>
 * Only tables are tracked here — functions and aggregates are embedded directly in the plan
 * since they are assumed to be thread-safe (stateless invoke, fresh accumulators).
 */
public interface SymbolTable {

    /**
     * Returns the number of catalogs referenced in this plan.
     * @return number of catalogs
     */
    int catalogCount();

    /**
     * Returns the catalog name for the given catalog ID.
     * @param catalogId the catalog identifier (0-indexed)
     * @return catalog name
     */
    @NotNull
    String getCatalogName(int catalogId);

    /**
     * Returns the table entries for a given catalog.
     * Table identifiers start at 0 and increment by one for each distinct table referenced in the plan.
     * The returned list is indexed by table ID (i.e., {@code getTables(catalogId).get(tableId)}).
     *
     * @param catalogId the catalog identifier
     * @return list of table entries, indexed by table ID
     */
    @NotNull
    List<TableEntry> getTables(int catalogId);

    /**
     * A table entry in the symbol table.
     */
    final class TableEntry {

        private final int id;
        private final Name name;
        private final PType schema;

        public TableEntry(int id, @NotNull Name name, @NotNull PType schema) {
            this.id = id;
            this.name = name;
            this.schema = schema;
        }

        public int getId() {
            return id;
        }

        @NotNull
        public Name getName() {
            return name;
        }

        @NotNull
        public PType getSchema() {
            return schema;
        }
    }
}

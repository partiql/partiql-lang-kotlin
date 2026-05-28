package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.catalog.Name;
import org.partiql.spi.function.RoutineSignature;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * A symbol table mapping plan-assigned integer IDs to catalog names, table names, and function signatures.
 * <p>
 * Returned alongside the {@link Plan} from the planner. Database owners use this to understand what each
 * integer ID in the plan represents and to build their {@link org.partiql.spi.catalog.ExecutionCatalog} accordingly.
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
     * @param catalogId the catalog identifier
     * @return list of table entries
     */
    @NotNull
    List<TableEntry> getTables(int catalogId);

    /**
     * Returns the function entries for a given catalog.
     * @param catalogId the catalog identifier
     * @return list of function entries
     */
    @NotNull
    List<FnEntry> getFunctions(int catalogId);

    /**
     * Returns the aggregate entries for a given catalog.
     * @param catalogId the catalog identifier
     * @return list of aggregate entries
     */
    @NotNull
    List<AggEntry> getAggregations(int catalogId);

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

    /**
     * A scalar function entry in the symbol table.
     */
    final class FnEntry {

        private final int id;
        private final Name name;
        private final RoutineSignature signature;

        public FnEntry(int id, @NotNull Name name, @NotNull RoutineSignature signature) {
            this.id = id;
            this.name = name;
            this.signature = signature;
        }

        public int getId() {
            return id;
        }

        @NotNull
        public Name getName() {
            return name;
        }

        @NotNull
        public RoutineSignature getSignature() {
            return signature;
        }
    }

    /**
     * An aggregate function entry in the symbol table.
     */
    final class AggEntry {

        private final int id;
        private final Name name;
        private final RoutineSignature signature;

        public AggEntry(int id, @NotNull Name name, @NotNull RoutineSignature signature) {
            this.id = id;
            this.name = name;
            this.signature = signature;
        }

        public int getId() {
            return id;
        }

        @NotNull
        public Name getName() {
            return name;
        }

        @NotNull
        public RoutineSignature getSignature() {
            return signature;
        }
    }
}

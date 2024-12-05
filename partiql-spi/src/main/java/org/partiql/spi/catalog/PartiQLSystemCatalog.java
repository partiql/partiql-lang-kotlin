package org.partiql.spi.catalog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.function.Aggregation;
import org.partiql.spi.function.Builtins;
import org.partiql.spi.function.Function;

import java.util.Collection;

/**
 * <p>
 * This implements the PartiQL System Catalog.
 * </p>
 * <p>
 * It provides the implementation for the PartiQL System Catalog, which is a built-in catalog
 * that provides access to the PartiQL language and its built-in functions and aggregations.
 * </p>
 * @see Session.Builder
 */
final class PartiQLSystemCatalog implements Catalog {

    /**
     * TODO
     */
    @NotNull
    private final String name;

    /**
     * Creates a new PartiQL System Catalog with the given name.
     * @param name the name of the PartiQL System Catalog
     */
    PartiQLSystemCatalog(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public Table getTable(@NotNull Session session, @NotNull Name name) {
        return null;
    }

    @Nullable
    @Override
    public Table getTable(@NotNull Session session, @NotNull Identifier identifier) {
        return null;
    }

    @NotNull
    @Override
    public Collection<Function> getFunctions(@NotNull Session session, @NotNull String name) {
        return Builtins.INSTANCE.getFunctions(name);
    }

    @NotNull
    @Override
    public Collection<Aggregation> getAggregations(@NotNull Session session, @NotNull String name) {
        return Builtins.INSTANCE.getAggregations(name);
    }
}

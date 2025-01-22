package org.partiql.spi.catalog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.function.AggOverload;
import org.partiql.spi.function.Builtins;
import org.partiql.spi.function.FnOverload;

import java.util.Collection;

/**
 * <p>
 * This package-private class implements the PartiQL System Catalog.
 * </p>
 * <p>
 * It provides the implementation for the PartiQL System Catalog, which is a built-in catalog
 * that provides access to the PartiQL language and its built-in functions and aggregations.
 * </p>
 * @see Session.Builder
 */
final class System implements Catalog {

    @NotNull
    private static final String NAME = "$system";

    /**
     * This is a package-private singleton.
     */
    static System INSTANCE = new System();

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @Nullable
    @Override
    public Table getTable(@NotNull Session session, @NotNull Name name) {
        return null;
    }

    @Nullable
    @Override
    public Name resolveTable(@NotNull Session session, @NotNull Identifier identifier) {
        return null;
    }

    @NotNull
    @Override
    public Collection<FnOverload> getFunctions(@NotNull Session session, @NotNull String name) {
        return Builtins.INSTANCE.getFunctions(name);
    }

    @NotNull
    @Override
    public Collection<AggOverload> getAggregations(@NotNull Session session, @NotNull String name) {
        return Builtins.INSTANCE.getAggregations(name);
    }
}

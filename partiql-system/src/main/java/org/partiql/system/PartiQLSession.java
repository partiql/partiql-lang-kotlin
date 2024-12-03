package org.partiql.system;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.spi.catalog.Catalogs;
import org.partiql.spi.catalog.Namespace;
import org.partiql.spi.catalog.Path;
import org.partiql.spi.catalog.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a package-private implementation of {@link Session}.
 * @see PartiQLSessionBuilder
 */
public final class PartiQLSession implements Session {

    @NotNull
    private final Catalogs _catalogs;

    @NotNull
    private final String identity;

    @Nullable
    private final Namespace systemCatalogNamespace;

    @NotNull
    private final String currentCatalog;

    @NotNull
    private final Namespace namespace;

    @NotNull
    private final Map<String, String> properties;

    PartiQLSession(boolean usesSystemCatalog, @NotNull String systemCatalogName, @NotNull String identity, @NotNull String catalog, @NotNull Namespace namespace, @NotNull Catalogs.Builder catalogs, @NotNull Map<String, String> properties) {
        this.systemCatalogNamespace = usesSystemCatalog ? Namespace.of(systemCatalogName) : null;
        this.identity = identity;
        this.currentCatalog = catalog;
        this.namespace = namespace;
        this.properties = properties;
        if (usesSystemCatalog) {
            catalogs.add(new PartiQLSystemCatalog(systemCatalogName));
        }
        this._catalogs = catalogs.build();
    }

    @NotNull
    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @NotNull
    @Override
    public Path getPath() {
        return Path.of(getNamespace(), systemCatalogNamespace);
    }

    @NotNull
    @Override
    public String getIdentity() {
        return identity;
    }

    @NotNull
    @Override
    public String getCatalog() {
        return currentCatalog;
    }

    @NotNull
    @Override
    public Catalogs getCatalogs() {
        return _catalogs;
    }

    @NotNull
    @Override
    public Namespace getNamespace() {
        return namespace;
    }

    /**
     * 
     * @return a session only holding the system catalog.
     */
    @NotNull
    public static Session empty() {
        return new PartiQLSession(
                true,
                "$pql_system",
                "unknown",
                "$pql_system",
                Namespace.of(),
                Catalogs.builder(),
                new HashMap<>()
        );
    }
}

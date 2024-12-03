package org.partiql.system;

import org.jetbrains.annotations.NotNull;
import org.partiql.spi.catalog.Catalog;
import org.partiql.spi.catalog.Catalogs;
import org.partiql.spi.catalog.Namespace;
import org.partiql.spi.catalog.Session;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This creates a {@link Session} while also installing the PartiQL System Catalog by default (unless
 * {@link PartiQLSessionBuilder#noSystemCatalog()} is specified). The resulting {@link Session} will append the System
 * Catalog to the end of the {@link Session#getPath()}.
 * @see PartiQLSessionBuilder#noSystemCatalog()
 * @see PartiQLSessionBuilder#systemCatalogName(String)
 */
public final class PartiQLSessionBuilder {

    private String identity = "unknown";
    private String catalog = null;
    private final Catalogs.Builder catalogs = Catalogs.builder();
    private Namespace namespace = Namespace.empty();
    private final Map<String, String> properties = new HashMap<>();
    private boolean usesSystemCatalog = true;
    private String systemCatalogName = "$partiql_system";

    /**
     * TODO
     * @param identity TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder identity(String identity) {
        this.identity = identity;
        return this;
    }

    /**
     * Removes the system catalog from the session.
     * @return the builder
     */
    @NotNull
    public PartiQLSessionBuilder noSystemCatalog() {
        this.usesSystemCatalog = false;
        return this;
    }

    /**
     * Sets the name of the system catalog.
     * @param name the desired name
     * @return the builder
     */
    @NotNull
    public PartiQLSessionBuilder systemCatalogName(@NotNull String name) {
        this.systemCatalogName = name;
        return this;
    }

    /**
     * TODO
     * @param catalog TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder catalog(String catalog) {
        this.catalog = catalog;
        return this;
    }

    /**
     * TODO
     * @param namespace TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder namespace(Namespace namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * TODO
     * @param levels TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder namespace(String... levels) {
        this.namespace = Namespace.of(levels);
        return this;
    }

    /**
     * TODO
     * @param levels TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder namespace(Collection<String> levels) {
        this.namespace = Namespace.of(levels);
        return this;
    }

    /**
     * TODO
     * @param name TODO
     * @param value TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder property(String name, String value) {
        this.properties.put(name, value);
        return this;
    }

    /**
     * TODO
     * @param catalogs TODO
     * @return TODO
     */
    @NotNull
    public PartiQLSessionBuilder catalogs(Catalog... catalogs) {
        for (Catalog catalog : catalogs) {
            this.catalogs.add(catalog);
        }
        return this;
    }

    /**
     * TODO
     * @return TODO
     */
    @NotNull
    public Session build() {
        return new PartiQLSession(usesSystemCatalog, systemCatalogName, identity, catalog, namespace, catalogs, properties);
    }
}

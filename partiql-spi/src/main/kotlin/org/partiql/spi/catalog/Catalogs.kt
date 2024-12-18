package org.partiql.spi.catalog

import org.partiql.spi.catalog.impl.StandardCatalogs

/**
 * Catalogs is used to provide the default catalog and possibly others by name.
 */
public interface Catalogs {

    /**
     * Returns a catalog by name (single identifier).
     */
    public fun getCatalog(name: String, ignoreCase: Boolean = false): Catalog?

    /**
     * Factory methods and builder.
     */
    public companion object {

        @JvmStatic
        public fun of(vararg catalogs: Catalog): Catalogs = of(catalogs.toList())

        @JvmStatic
        public fun of(catalogs: Collection<Catalog>): Catalogs {
            return builder().apply { catalogs.forEach { add(it) } }.build()
        }

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Lombok java-style builder for a default [Catalogs] implementation.
     */
    public class Builder {

        private val catalogs = mutableMapOf<String, Catalog>()

        /**
         * Adds this catalog, overwriting any existing one with the same name.
         */
        public fun add(catalog: Catalog): Builder = this.apply {
            catalogs[catalog.getName()] = catalog
        }

        public fun build(): Catalogs = StandardCatalogs(catalogs)
    }
}

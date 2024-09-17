package org.partiql.spi.catalog

/**
 * Catalogs is used to provide the default catalog and possibly others by name.
 */
public interface Catalogs {

    /**
     * Returns a catalog by name (single identifier).
     */
    public fun getCatalog(name: String, ignoreCase: Boolean = false): Catalog?

    /**
     * Returns a list of all available catalogs.
     */
    public fun listCatalogs(): Collection<Catalog> = listOf()

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
     * Java-style builder for a default [Catalogs] implementation.
     */
    public class Builder {

        private val catalogs = mutableMapOf<String, Catalog>()

        /**
         * Adds this catalog, overwriting any existing one with the same name.
         */
        public fun add(catalog: Catalog): Builder = this.apply {
            catalogs[catalog.getName()] = catalog
        }

        public fun build(): Catalogs {

            return object : Catalogs {

                override fun getCatalog(name: String, ignoreCase: Boolean): Catalog? {
                    // search
                    if (ignoreCase) {
                        var match: Catalog? = null
                        for (catalog in catalogs.values) {
                            if (catalog.getName().equals(name, ignoreCase = true)) {
                                if (match != null) {
                                    // TODO exceptions for ambiguous catalog name lookup
                                    error("Catalog name is ambiguous, found more than one match.")
                                } else {
                                    match = catalog
                                }
                            }
                        }
                        return match
                    }
                    // lookup
                    return catalogs[name]
                }

                override fun listCatalogs(): Collection<Catalog> = catalogs.values
            }
        }
    }
}

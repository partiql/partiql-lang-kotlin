package org.partiql.planner.catalog

/**
 * Catalogs is used to provide the default catalog and possibly others by name.
 */
public interface Catalogs {

    /**
     * Returns the default catalog. Required.
     */
    public fun default(): Catalog

    /**
     * Returns a catalog by name (single identifier).
     */
    public fun get(name: String, ignoreCase: Boolean = false): Catalog? {
        val default = default()
        return if (name.equals(default.getName(), ignoreCase)) {
            default
        } else {
            null
        }
    }

    /**
     * Returns a list of all available catalogs.
     */
    public fun list(): Collection<Catalog> = listOf(default())

    /**
     * Factory methods and builder.
     */
    public companion object {

        @JvmStatic
        public fun of(vararg catalogs: Catalog): Catalogs = of(catalogs.toList())

        @JvmStatic
        public fun of(catalogs: Collection<Catalog>): Catalogs {
            if (catalogs.isEmpty()) {
                error("Cannot create `Catalogs` with empty catalogs list.")
            }
            return builder().apply { catalogs.forEach { add(it) } }.build()
        }

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Java-style builder for a default [Catalogs] implementation.
     */
    public class Builder {

        private var default: Catalog? = null
        private val catalogs = mutableMapOf<String, Catalog>()

        /**
         * Sets the default catalog.
         */
        public fun default(default: Catalog): Builder = this.apply {
            this.default = default
            catalogs[default.getName()] = default
        }

        /**
         * Adds this catalog, overwriting any existing one with the same name.
         */
        public fun add(catalog: Catalog): Builder = this.apply {
            if (default == null) {
                this.default = catalog
            }
            catalogs[catalog.getName()] = catalog
        }

        public fun build(): Catalogs {

            val default = default ?: error("Default catalog is required")

            return object : Catalogs {

                override fun default(): Catalog = default

                override fun get(name: String, ignoreCase: Boolean): Catalog? {
                    if (ignoreCase) {
                        // search
                        var match: Catalog? = null
                        for (catalog in list()) {
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

                override fun list(): Collection<Catalog> = catalogs.values
            }
        }
    }
}

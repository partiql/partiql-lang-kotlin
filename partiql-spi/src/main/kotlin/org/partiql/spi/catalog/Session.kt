package org.partiql.spi.catalog

/**
 * Session is used for authorization and name resolution.
 */
public interface Session {

    /**
     * Returns the caller identity as a string; accessible via CURRENT_USER.
     */
    public fun getIdentity(): String

    /**
     * Returns the current [Catalog]; accessible via the CURRENT_CATALOG session variable.
     */
    public fun getCatalog(): String

    /**
     * Returns the catalog provider for this session.
     */
    public fun getCatalogs(): Catalogs

    /**
     * Returns the current [Namespace]; accessible via the CURRENT_NAMESPACE session variable.
     */
    public fun getNamespace(): Namespace

    /**
     * Returns the current [Path]; accessible via the PATH and CURRENT_PATH session variables.
     *
     * Default implementation returns the current namespace.
     */
    public fun getPath(): Path = Path.of(getNamespace())

    /**
     * Arbitrary session properties that may be used in planning or custom plan passes.
     */
    public fun getProperties(): Map<String, String> = emptyMap()

    /**
     * Factory methods and builder.
     */
    public companion object {

        /**
         * Returns a [Session] with only the "empty" catalog implementation.
         */
        @JvmStatic
        public fun empty(): Session {
            val catalog = Catalog.empty("empty")
            return builder().catalog(catalog.getName()).catalogs(catalog).build()
        }

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Java-style builder for a default [Session] implementation.
     */
    public class Builder {

        private var identity: String = "unknown"
        private var catalog: String? = null
        private var systemCatalog: Catalog = PartiQLSystemCatalog.INSTANCE
        private var catalogs: Catalogs.Builder = Catalogs.builder()
        private var namespace: Namespace = Namespace.empty()
        private var properties: MutableMap<String, String> = mutableMapOf()

        public fun identity(identity: String): Builder {
            this.identity = identity
            return this
        }

        public fun catalog(catalog: String?): Builder {
            this.catalog = catalog
            return this
        }

        public fun namespace(namespace: Namespace): Builder {
            this.namespace = namespace
            return this
        }

        public fun namespace(vararg levels: String): Builder {
            this.namespace = Namespace.of(*levels)
            return this
        }

        public fun namespace(levels: Collection<String>): Builder {
            this.namespace = Namespace.of(levels)
            return this
        }

        public fun property(name: String, value: String): Builder {
            this.properties[name] = value
            return this
        }

        /**
         * Adds and designates a catalog to always be on the SQL-Path. This [catalog] provides all built-in functions
         * to the system at hand.
         * If this is never invoked, a default system catalog is provided.
         */
        public fun system(catalog: Catalog): Builder {
            this.systemCatalog = catalog
            return this
        }

        /**
         * Adds catalogs to this session.
         */
        public fun catalogs(vararg catalogs: Catalog): Builder {
            for (catalog in catalogs) {
                this.catalogs.add(catalog)
            }
            return this
        }

        public fun build(): Session = object : Session {

            private val _catalogs: Catalogs
            private val systemCatalogNamespace: Namespace = Namespace.of(systemCatalog.getName())

            init {
                require(catalog != null) { "Session catalog must be set" }
                catalogs.add(systemCatalog)
                _catalogs = catalogs.build()
            }

            override fun getIdentity(): String = identity
            override fun getCatalog(): String = catalog!!
            override fun getCatalogs(): Catalogs = _catalogs
            override fun getNamespace(): Namespace = namespace

            override fun getPath(): Path {
                val currentNamespace = getNamespace()
                return Path.of(currentNamespace, systemCatalogNamespace)
            }
        }
    }
}

package org.partiql.planner.catalog

import org.partiql.spi.connector.ConnectorMetadata

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
     *
     * TODO replace with org.partiql.planner.catalog.Catalogs
     */
    public fun getCatalogs(): Map<String, ConnectorMetadata>

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
         * Returns an empty [Session] with the provided [catalog] and an empty provider.
         */
        @JvmStatic
        public fun empty(catalog: String): Session = object : Session {
            override fun getIdentity(): String = "unknown"
            override fun getCatalog(): String = catalog
            override fun getCatalogs(): Map<String, ConnectorMetadata> = emptyMap()
            override fun getNamespace(): Namespace = Namespace.empty()
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
        private var catalogs: MutableMap<String, ConnectorMetadata> = mutableMapOf()
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
         * Adds catalogs to this session like the old Map<String, ConnectorMetadata>.
         *
         * TODO replace with org.partiql.planner.catalog.Catalog.
         */
        public fun catalogs(vararg catalogs: Pair<String, ConnectorMetadata>): Builder {
            for ((name, metadata) in catalogs) this.catalogs[name] = metadata
            return this
        }

        public fun build(): Session = object : Session {

            init {
                require(catalog != null) { "Session catalog must be set" }
            }

            override fun getIdentity(): String = identity
            override fun getCatalog(): String = catalog!!
            override fun getCatalogs(): Map<String, ConnectorMetadata> = catalogs
            override fun getNamespace(): Namespace = namespace
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
    public fun getPath(): Path = Path.of(Namespace.of(getCatalog()).append(*getNamespace().getLevels()))

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
            return builder().catalog(System.INSTANCE.getName()).catalogs().build()
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
        private var system: Catalog = System.INSTANCE
        private var catalogs: Catalogs.Builder = Catalogs.builder()
        private var catalogNames: MutableList<String> = mutableListOf()
        private var namespace: Namespace = Namespace.empty()
        private var path: Path? = null
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

        public fun namespace(levels: List<String>): Builder {
            this.namespace = Namespace.of(levels)
            return this
        }

        /**
         * Sets the ordered path used to resolve unqualified routines.
         *
         * Each entry contains a catalog name followed by zero or more namespace levels. The supplied entries replace
         * the default current catalog and namespace entry. The configured system catalog root is appended at build
         * time only when the path does not already contain it.
         */
        public fun path(vararg namespaces: Namespace): Builder = path(Path.of(*namespaces))

        /**
         * Sets the ordered path used to resolve unqualified routines.
         *
         * The supplied [path] is snapshotted. Qualified routine calls and table resolution do not use this path.
         */
        public fun path(path: Path): Builder {
            this.path = Path.of(*path.toList().toTypedArray())
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
            this.system = catalog
            return this
        }

        /**
         * Adds catalogs to this session.
         */
        public fun catalogs(vararg catalogs: Catalog): Builder {
            for (catalog in catalogs) {
                val catalogName = catalog.getName()
                if (this.catalogNames.contains(catalogName)) {
                    throw IllegalStateException("Catalog names must be unique: $catalogName")
                }
                this.catalogNames.add(catalogName)
                this.catalogs.add(catalog)
            }
            return this
        }

        public fun build(): Session = object : Session {

            private val _catalogs: Catalogs
            private val systemCatalogNamespace: Namespace = Namespace.of(system.getName())
            private val _path: Path?

            init {
                require(catalog != null) { "Session catalog must be set" }
                catalogs.add(system)
                _catalogs = catalogs.build()
                val configuredPath = path
                _path = if (configuredPath == null) {
                    null
                } else {
                    val entries = configuredPath.toList()
                    val effectiveEntries = if (systemCatalogNamespace in entries) {
                        entries
                    } else {
                        entries + listOf(systemCatalogNamespace)
                    }
                    Path.of(*effectiveEntries.toTypedArray())
                }
            }

            override fun getIdentity(): String = identity
            override fun getCatalog(): String = catalog!!
            override fun getCatalogs(): Catalogs = _catalogs
            override fun getNamespace(): Namespace = namespace

            override fun getPath(): Path {
                val configuredPath = _path
                if (configuredPath != null) {
                    return configuredPath
                }
                val currentNamespace = Namespace.of(getCatalog(), *getNamespace().getLevels())
                return Path.of(currentNamespace, systemCatalogNamespace)
            }

            override fun getProperties(): Map<String, String> {
                return properties
            }
        }
    }
}

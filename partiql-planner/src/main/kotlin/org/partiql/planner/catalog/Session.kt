package org.partiql.planner.catalog

/**
 * Session is used for authorization and name resolution.
 */
public interface Session {

    /**
     * Returns the caller identity as a string; accessible via CURRENT_USER.
     */
    public fun getIdentity(): String

    /**
     * Returns the current [Namespace]; accessible via the NAMESPACE session variable.
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

        @JvmStatic
        public fun empty(): Session = object : Session {
            override fun getIdentity(): String = "unknown"
            override fun getNamespace(): Namespace = Namespace.root()
        }

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Java-style builder for a default [Session] implementation.
     */
    public class Builder {

        private var identity: String = "unknown"
        private var namespace: Namespace = Namespace.root()
        private var properties: MutableMap<String, String> = mutableMapOf()

        public fun identity(identity: String): Builder {
            this.identity = identity
            return this
        }

        public fun namespace(namespace: Namespace): Builder {
            this.namespace = namespace
            return this
        }

        public fun property(name: String, value: String): Builder {
            this.properties[name] = value
            return this
        }

        public fun build(): Session = object : Session {
            override fun getIdentity(): String = identity
            override fun getNamespace(): Namespace = namespace
        }
    }
}

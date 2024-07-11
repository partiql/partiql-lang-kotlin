package org.partiql.planner.catalog

/**
 * Session is used for authorization and name resolution.
 */
public interface Session {

    public companion object {

        private val EMPTY = object : Session {
            override fun getIdentity(): String = "unknown"
            override fun getNamespace(): Namespace = Namespace.root()
        }

        @JvmStatic
        public fun empty(): Session = EMPTY

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

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
     * Java-style session builder.
     */
    public class Builder {

        private var identity: String = "unknown"
        private var namespace: Namespace = Namespace.root()
        private var properties: MutableMap<String, String> = mutableMapOf()

        public fun identity(identity: String): Builder = this.apply { this.identity = identity }

        public fun namespace(namespace: Namespace): Builder = this.apply { this.namespace = namespace }

        public fun property(name: String, value: String): Builder = this.apply { this.properties[name] = value }

        public fun build(): Session = object : Session {
            override fun getIdentity(): String = identity
            override fun getNamespace(): Namespace = namespace
        }
    }
}

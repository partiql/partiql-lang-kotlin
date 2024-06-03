package org.partiql.planner.metadata

/**
 * The PartiQL-session metadata.
 *
 * NOTE: It is not clear to me (Robert) where this should live (in metadata?).
 */
public interface Session {

    /**
     * Returns the current namespace for this session.
     */
    public fun getNamespace(): Namespace

    /**
     * Returns the current path for routine lookup.
     */
    public fun getPath(): List<Name>

    /**
     * Default [Session] implementation.
     *
     * @property namespace  The current namespace for this session.
     * @property path   The current path for unqualified name lookup.
     */
    public data class Base(
        @JvmField public val namespace: Namespace,
        @JvmField public val path: List<Name>,
    ) : Session {
        override fun getNamespace(): Namespace = namespace
        override fun getPath(): List<Name> = path
    }
}

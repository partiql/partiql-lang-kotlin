package org.partiql.planner.metadata

/**
 * A [Namespace] is a namespace for the following objects; top-level namespaces are called "Catalogs".
 *
 * . Tables
 *      . Base
 *      . View
 *      . Index
 * . Routines
 *      . Functions
 *      . Procedures
 * . Namespaces
 * . Types
 *
 * See, https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/schema/Schema.java
 */
public interface Namespace {

    /**
     * The [Namespace] name.
     */
    public fun getName(): String

    /**
     * Get a table by name.
     *
     * @param name  The case-sensitive [Table] name.
     * @return The [Table] or null if not found.
     */
    public fun getTable(name: String): Table? = null

    /**
     * Get a function's variants by name.
     *
     * @param name  The case-sensitive [Function] name.
     * @return A collection of all [Function]s in the current namespace with this name.
     */
    public fun getFunctions(name: String): Collection<Function> = DEFAULT_FUNCTIONS

    /**
     * Get a sub-namespace by name.
     *
     * @param name
     * @return
     */
    public fun getNamespace(name: String): Namespace? = null

    /**
     * Get all sub-namespaces.
     */
    public fun getNamespaces(): Collection<Namespace> = DEFAULT_SCOPES

    /**
     * Memoized defaults.
     */
    private companion object {
        val DEFAULT_FUNCTIONS = emptyList<Function>()
        val DEFAULT_SCOPES = emptyList<Namespace>()
    }
}

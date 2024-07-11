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
    public fun get(identifier: Identifier): Catalog? {
        val default = default()
        return if (identifier.matches(default.getName())) {
            default
        } else {
            null
        }
    }

    /**
     * Returns a list of all available catalogs.
     */
    public fun list(): Collection<Catalog> = listOf(default())
}

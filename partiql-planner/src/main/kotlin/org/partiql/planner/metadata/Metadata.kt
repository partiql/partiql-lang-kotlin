package org.partiql.planner.metadata

/**
 * Top-level metadata interface for access to object descriptors.
 */
public interface Metadata {

    /**
     * The root namespace.
     */
    public fun getNamespace(): Namespace
}

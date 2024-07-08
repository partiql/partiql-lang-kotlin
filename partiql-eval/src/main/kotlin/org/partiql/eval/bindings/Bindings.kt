package org.partiql.eval.bindings

/**
 * Top-level interface for loading data into the engine.
 */
interface Bindings {

    /**
     * Get nested bindings for the given name (or null).
     */
    fun getBindings(name: String): Bindings?

    /**
     * Get a binding for the given name (or null).
     */
    fun getBinding(name: String): Binding?
}

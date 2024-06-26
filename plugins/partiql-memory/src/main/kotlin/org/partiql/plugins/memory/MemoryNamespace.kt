package org.partiql.plugins.memory

import org.partiql.planner.metadata.Namespace
import org.partiql.planner.metadata.Table
import org.partiql.types.PType

/**
 * Namespace implementation.
 */
public class MemoryNamespace private constructor(
    private val name: String,
    private val tables: Map<String, Table>,
    private val namespaces: Map<String, Namespace>,
) : Namespace {

    override fun getName(): String = name
    override fun getTable(name: String): Table? = tables[name]
    override fun getNamespace(name: String): Namespace? = namespaces[name]
    override fun getNamespaces(): Collection<Namespace> = namespaces.values

    public class Builder internal constructor() {

        private var name: String? = null
        private var tables: MutableMap<String, Table> = mutableMapOf()
        private var namespaces: MutableMap<String, Namespace> = mutableMapOf()

        public fun name(name: String): Builder = apply { this.name = name }

        public fun define(name: String, type: PType): Builder = apply { tables[name] = MemoryTable.of(type) }

        public fun define(namespace: Namespace): Builder = apply { namespaces[namespace.getName()] = namespace }

        public fun build(): MemoryNamespace = MemoryNamespace(name!!, tables, namespaces)
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
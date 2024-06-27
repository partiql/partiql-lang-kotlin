package org.partiql.plugins.memory

import org.partiql.planner.metadata.Namespace
import org.partiql.planner.metadata.Table

/**
 * Namespace implementation.
 */
public class MemoryNamespace private constructor(
    private val name: String,
    private val tables: Map<String, MemoryTable>,
    private val namespaces: Map<String, MemoryNamespace>,
) : Namespace {
    override fun getName(): String = name
    override fun getTable(name: String): Table? = tables[name]
    override fun getNamespace(name: String): Namespace? = namespaces[name]
    override fun getNamespaces(): Collection<Namespace> = namespaces.values

    public class Builder internal constructor() {

        private var name: String? = null
        private var tables: MutableMap<String, MemoryTable> = mutableMapOf()
        private var namespaces: MutableMap<String, MemoryNamespace> = mutableMapOf()

        public fun name(name: String): Builder = apply { this.name = name }

        public fun defineTable(name: String, table: MemoryTable): Builder = apply { tables[name] = table }

        public fun defineNamespace(namespace: MemoryNamespace): Builder = apply { namespaces[namespace.getName()] = namespace }

        public fun build(): MemoryNamespace = MemoryNamespace(name!!, tables, namespaces)
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}

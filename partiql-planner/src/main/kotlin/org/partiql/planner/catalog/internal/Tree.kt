package org.partiql.planner.catalog.internal

import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Table

/**
 * Helper for lookups.
 */
internal class Tree {

    private val root = Node(null, mutableMapOf())

    private class Node(
        @JvmField var table: Table?,
        @JvmField val children: MutableMap<String, Node>,
    ) {

        fun get(name: String): Node? = children[name]

        fun insert(name: String): Node {
            var child = children[name]
            if (child == null) {
                child = Node(null, mutableMapOf())
                children[name] = child
            }
            return child
        }
    }

    private fun get(namespace: Namespace):  Node? {
        var curr: Node = root
        for (name in namespace) {
            curr = curr.get(name) ?: return null
        }
        return curr
    }

    /**
     * Insert the table at the given namespace.
     */
    fun insert(namespace: Namespace, table: Table) {
        var curr = root
        for (name in namespace) {
            curr = curr.insert(name)
        }
        val name = table.getName()
        curr = curr.insert(name)
        curr.table = table
    }

    /**
     * List tables at the current namespace.
     */
    fun listTables(namespace: Namespace): List<Name> {
        val ns = get(namespace) ?: return emptyList()
        return ns.children.values.mapNotNull {
            if (it.table == null) {
                null
            } else {
                Name(namespace, it.table!!.getName())
            }
        }
    }

    /**
     * List namespaces at the current namespace.
     */
    fun listNamespaces(namespace: Namespace): List<Namespace> {
        val ns = get(namespace) ?: return emptyList()
        return ns.children.mapNotNull { (key, value) ->
            if (value.table != null) {
                null
            } else {
                namespace.concat(key)
            }
        }
    }

    /**
     * Lookup the namespace, then get the table.
     */
    fun getTable(name: Name): Table? {
        return get(name.getNamespace())?.get(name.getName())?.table
    }
}

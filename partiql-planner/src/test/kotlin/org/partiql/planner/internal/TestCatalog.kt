package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Routine
import org.partiql.planner.catalog.Session
import org.partiql.planner.catalog.Table
import org.partiql.types.PType

public class TestCatalog private constructor(
    private val name: String,
    private val root: Tree,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        return null
    }

    /**
     * TODO this is currently case-sensitive.
     */
    override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
        val matched = mutableListOf<String>()
        var curr: Tree = root
        for (part in identifier) {
            val text = part.getText()
            curr = curr.get(text) ?: break
            matched.add(text)
        }
        if (curr.table == null) {
            return null
        }
        // calculate matched
        return Table.Handle(
            name = Name.of(matched),
            table = curr.table!!
        )
    }

    // TODO
    override fun listTables(session: Session, namespace: Namespace): Collection<Name> {
        return emptyList()
    }

    // TODO
    override fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> {
        return emptyList()
    }

    // TODO
    override fun getRoutines(session: Session, name: Name): Collection<Routine> {
        return emptyList()
    }

    private class Tree(
        @JvmField val name: String,
        @JvmField var table: Table?,
        @JvmField val children: MutableMap<String, Tree>,
    ) {
        fun contains(name: String) = children.contains(name)
        fun get(name: String): Tree? = children[name]
        fun getOrPut(name: String): Tree = children.getOrPut(name) { Tree(name, null, mutableMapOf()) }
    }

    override fun toString(): String = buildString {
        for (child in root.children.values) {
            append(toString(child))
        }
    }

    private fun toString(tree: Tree, prefix: String? = null): String = buildString {
        val pre = if (prefix != null) prefix + "." + tree.name else tree.name
        appendLine(pre)
        for (child in tree.children.values) {
            append(toString(child, pre))
        }
    }

    companion object {

        @JvmStatic
        fun empty(name: String): TestCatalog = TestCatalog(name, Tree(name, null, mutableMapOf()))

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * Perhaps this will be moved to Catalog.
     */
    class Builder {

        private var name: String? = null
        private val root = Tree(".", null, mutableMapOf())

        fun name(name: String): Builder {
            this.name = name
            return this
        }

        fun createTable(name: String, schema: PType): Builder {
            return createTable(Name.of(name), schema)
        }

        fun createTable(name: Name, schema: PType): Builder {
            var curr = root
            for (part in name) {
                // upsert namespaces
                curr = curr.getOrPut(part)
            }
            curr.table = Table.of(name.getName(), schema)
            return this
        }

        fun build(): Catalog {
            if (name == null) {
                error("Catalog must have a name")
            }
            return TestCatalog(name!!, root)
        }
    }
}

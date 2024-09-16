package org.partiql.planner.internal

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.types.PType

/**
 * Basic catalog implementation used for testing; consider merging with MemoryConnector?
 */
public class TestCatalog private constructor(
    private val name: String,
    private val root: Tree,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        return null
    }

    override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
        val matched = mutableListOf<String>()
        var curr: Tree = root
        for (part in identifier) {
            curr = curr.get(part) ?: break
            matched.add(curr.name)
        }
        if (curr.table == null) {
            return null
        }
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

    private class Tree(
        @JvmField val name: String,
        @JvmField var table: Table?,
        @JvmField val children: MutableMap<String, Tree>,
    ) {

        /**
         * TODO ambiguous binding error?
         */
        fun get(part: Identifier.Part): Tree? {
            // regular, search insensitively
            if (part.isRegular()) {
                for (child in children.values) {
                    if (part.matches(child.name)) {
                        return child
                    }
                }
            }
            // delimited, search exact
            return children[part.getText()]
        }

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

        fun createTable(name: Name, schema: PType): Builder {
            var curr = root
            for (part in name) {
                // upsert namespaces
                curr = curr.getOrPut(part)
            }
            curr.table = Table.empty(name.getName(), schema)
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
